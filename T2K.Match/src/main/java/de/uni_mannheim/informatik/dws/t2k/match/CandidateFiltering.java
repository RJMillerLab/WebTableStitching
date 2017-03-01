/** 
 *
 * Copyright (C) 2015 Data and Web Science Group, University of Mannheim, Germany (code@dwslab.de)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.uni_mannheim.informatik.dws.t2k.match;

import java.util.Map;
import java.util.Set;

import de.uni_mannheim.informatik.dws.t2k.match.data.MatchableTableColumn;
import de.uni_mannheim.informatik.dws.t2k.match.data.MatchableTableRow;
import de.uni_mannheim.informatik.wdi.matching.MatchingEngine;
import de.uni_mannheim.informatik.wdi.model.Correspondence;
import de.uni_mannheim.informatik.wdi.model.ResultSet;
import de.uni_mannheim.informatik.wdi.processing.DataProcessingEngine;
import de.uni_mannheim.informatik.wdi.processing.DatasetIterator;
import de.uni_mannheim.informatik.wdi.processing.RecordMapper;

/**
 * 
 * Removes the candidates that do not belong to the selected classes.
 * 
 * @author Oliver Lehmberg (oli@dwslab.de)
 *
 */
public class CandidateFiltering {

	private MatchingEngine<MatchableTableRow, MatchableTableColumn> matchingEngine;
	private Map<Integer, Set<String>> classesPerTable;
	private Map<Integer, String> classIndices;
	private ResultSet<Correspondence<MatchableTableRow, MatchableTableColumn>> instanceCorrespondences;
	

	public CandidateFiltering(MatchingEngine<MatchableTableRow, MatchableTableColumn> matchingEngine, Map<Integer, Set<String>> classesPerTable, Map<Integer, String> classIndices, ResultSet<Correspondence<MatchableTableRow, MatchableTableColumn>> instanceCorrespondences) {
		this.matchingEngine = matchingEngine;
		this.classesPerTable = classesPerTable;
		this.instanceCorrespondences = instanceCorrespondences;
		this.classIndices = classIndices;
	}
	
	public ResultSet<Correspondence<MatchableTableRow, MatchableTableColumn>> run() {
		DataProcessingEngine proc = matchingEngine.getProcessingEngine();
		
		RecordMapper<Correspondence<MatchableTableRow, MatchableTableColumn>, Correspondence<MatchableTableRow, MatchableTableColumn>> filterInvalidClasses = new RecordMapper<Correspondence<MatchableTableRow,MatchableTableColumn>, Correspondence<MatchableTableRow,MatchableTableColumn>>() {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void mapRecord(Correspondence<MatchableTableRow, MatchableTableColumn> record,
					DatasetIterator<Correspondence<MatchableTableRow, MatchableTableColumn>> resultCollector) {

				// get the class name of the candidate
				String className = classIndices.get(record.getSecondRecord().getTableId()); 
				
				// get the valid classes for the web table
				Set<String> validClasses = classesPerTable.get(record.getFirstRecord().getTableId());
				
				// check if it is a valid class
				if(validClasses.contains(className)) {
					
					// if yes, keep the candidate
					resultCollector.next(record);
					
				}
			}
		};
		return proc.transform(instanceCorrespondences, filterInvalidClasses);
	}
	
}
