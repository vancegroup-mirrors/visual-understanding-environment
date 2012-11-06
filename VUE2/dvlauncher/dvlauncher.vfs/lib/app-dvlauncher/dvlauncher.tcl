package provide app-dvlauncher 1.0

package require telnet

set host localhost
set port 7778

set ttyBuffer {}
proc readTty {tty} {
    if {[catch {set input [read $tty]} err]} {
	error $err $::errorInfo
    }
    append ::ttyBuffer $input
    set ::telnetEvent readTty
}

proc readStdin {} {
    if {[eof stdin]} {
      set ::telnetEvent EOF
      return
    }
    if {[catch {set input [gets stdin]} err]} {
	error $err $::errorInfo
    }
    # Add a carriage return because if you are interactive on windows, then
    # the carriage return is eaten by the console
    append ::ttyBuffer $input "\r"
    set ::telnetEvent readStdin
}

proc tcpRead {pt} {
    if {[eof $pt]} {
      set ::telnetEvent EOF
      return
    }
    append ::telnetBuffer [telnet::read $pt]
    set ::telnetEvent tcpRead
}

if [catch {set pt [telnet::open $host $port]}] {
	cd [file dirname [file dirname [file dirname $tcl_library]]]
	set cmd [lindex [glob designVUE*.jar] 0]
	set cmd "javaw -jar $cmd"
	open |$cmd
	while {[catch {set pt [telnet::open $host $port]}]} {
		after 1000
	}
	after 1000
	fconfigure $pt -blocking false -buffering none
	fileevent $pt readable [list tcpRead $pt]
	fileevent stdin readable [list readStdin]
	fconfigure stdout -buffering none
	telnet::write $pt "import tufts.vue.*;VUE.FilesToOpen.add(\"[regsub -all / [file normalize [lindex $argv 0]] //]\");\n"
	#telnet::write $pt "import tufts.vue.*;VUE.displayMap(new File(\"[regsub -all / [file normalize [lindex $argv 0]] //]\"));\n"
	exit
}

fconfigure $pt -blocking false -buffering none
fileevent $pt readable [list tcpRead $pt]
fileevent stdin readable [list readStdin]
fconfigure stdout -buffering none

telnet::write $pt "import tufts.vue.*;VUE.displayMap(new File(\"[regsub -all / [file normalize [lindex $argv 0]] //]\"));\n"
exit
