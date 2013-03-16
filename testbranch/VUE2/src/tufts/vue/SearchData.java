/*
* Copyright 2003-2010 Tufts University  Licensed under the
 * Educational Community License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * 
 * http://www.osedu.org/licenses/ECL-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package tufts.vue;

import java.util.List;

import edu.tufts.vue.metadata.VueMetadataElement;
public class SearchData {

	private String searchSaveName;
	
	private String searchType;

	private String mapType;

	private String resultType;

	private String andOrType;

	private List<VueMetadataElement> dataList;

	public SearchData() {

	}

	public SearchData(String searchSaveName, String searchType, String mapType, String resultType,
			String andOrType, List<VueMetadataElement> dataList) {

		super();
		
		this.searchSaveName = searchSaveName;
		
		this.searchType = searchType;

		this.mapType = mapType;

		this.resultType = resultType;

		this.andOrType = andOrType;

		this.dataList = dataList;

	}
	
	public String getSearchSaveName() {

		return searchSaveName;

	}

	public void setSearchSaveName(String searchSaveName) {

		this.searchSaveName = searchSaveName;

	}
	
	public String getSearchType() {

		return searchType;

	}

	public void setSearchType(String searchType) {

		this.searchType = searchType;

	}

	public String getMapType() {

		return mapType;

	}

	public void setMapType(String mapType) {

		this.mapType = mapType;

	}

	public String getResultType() {

		return resultType;

	}

	public void setResultType(String resultType) {

		this.resultType = resultType;

	}

	public String getAndOrType() {

		return andOrType;

	}

	public void setAndOrType(String andOrType) {

		this.andOrType = andOrType;

	}

	public List<VueMetadataElement> getDataList() {

		return dataList;

	}

	public void setDataList(List<VueMetadataElement> dataList) {

		this.dataList = dataList;

	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		sb.append(" { Search Details --");

		sb.append("searchType: " + getSearchType());

		sb.append(", ");

		sb.append("mapType: " + getMapType());

		sb.append(", ");

		sb.append("resultType: " + getResultType());

		sb.append(", ");

		sb.append("andOrType: " + getAndOrType());
		
		sb.append(", ");

		sb.append("dataList: " + getDataList());

		sb.append(". } \n");

		return sb.toString();

	}

}