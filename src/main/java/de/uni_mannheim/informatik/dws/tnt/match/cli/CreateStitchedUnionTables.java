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
package de.uni_mannheim.informatik.dws.tnt.match.cli;

import java.io.File;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.HashMap;
import java.util.Iterator;
import java.io.*;

import com.beust.jcommander.Parameter;

import de.uni_mannheim.informatik.dws.tnt.match.DisjointHeaders;
import de.uni_mannheim.informatik.dws.tnt.match.data.MatchableTableColumn;
import de.uni_mannheim.informatik.dws.tnt.match.data.WebTables;
import de.uni_mannheim.informatik.dws.tnt.match.evaluation.N2NGoldStandardCreator;
import de.uni_mannheim.informatik.dws.tnt.match.matchers.CandidateKeyBasedMatcher;
import de.uni_mannheim.informatik.dws.tnt.match.matchers.DeterminantBasedMatcher;
import de.uni_mannheim.informatik.dws.tnt.match.matchers.EntityLabelBasedMatcher;
import de.uni_mannheim.informatik.dws.tnt.match.matchers.LabelBasedMatcher;
import de.uni_mannheim.informatik.dws.tnt.match.matchers.TableToTableMatcher;
import de.uni_mannheim.informatik.dws.tnt.match.matchers.ValueBasedMatcher;
import de.uni_mannheim.informatik.dws.tnt.match.stitching.StitchedUnionTables;
import de.uni_mannheim.informatik.dws.winter.matching.MatchingEngine;
import de.uni_mannheim.informatik.dws.winter.model.Correspondence;
import de.uni_mannheim.informatik.dws.winter.model.Matchable;
import de.uni_mannheim.informatik.dws.winter.processing.Processable;
import de.uni_mannheim.informatik.dws.winter.utils.Executable;
import de.uni_mannheim.informatik.dws.winter.webtables.Table;
import de.uni_mannheim.informatik.dws.winter.webtables.writers.JsonTableWriter;

/**
 * @author Oliver Lehmberg (oli@dwslab.de)
 *
 */
public class CreateStitchedUnionTables extends Executable {

	public static enum MatcherType {
		Trivial,
		NonTrivialPartial,
		NonTrivialFull,
		CandidateKey,
		Label,
		Entity
	}
	
	@Parameter(names = "-matcher")
	private MatcherType matcher = MatcherType.NonTrivialFull;
	
	@Parameter(names = "-web", required=true)
	private String webLocation;
	
	@Parameter(names = "-results", required=true)
	private String resultLocation;
	
	@Parameter(names = "-serialise")
	private boolean serialise;
	
	@Parameter(names = "-prepareGS")
	private boolean prepareGoldStandard = false;
	
	@Parameter(names = "-eval")
	private String evaluationLocation;
	
	public static void main(String[] args) throws Exception {
		CreateStitchedUnionTables app = new CreateStitchedUnionTables();
		
		if(app.parseCommandLine(CreateStitchedUnionTables.class, args)) {
			app.run();
		}
	}
	
	public void run() throws Exception {
		System.err.println("Loading Web Tables");
		// code added by FN
        String[] corpusFile = sourceLocation.list();


        WebTables web = WebTables.loadWebTables(new File(webLocation), true, true, false, serialise);
		//web.removeHorizontallyStackedTables();
	
        // print table map to a file
        writeMap(web.getTableIndices()); 
        //
		System.err.println("Matching Union Tables");
		Processable<Correspondence<MatchableTableColumn, Matchable>> schemaCorrespondences = runTableMatching(web);
	    // writing schema correspondences to a csv file 
        BufferedWriter w = new BufferedWriter(new FileWriter("/home/fnargesian/TABLE_UNION_OUTPUT/benchmark-v5/stitching_results.csv"));
         System.err.println("Writing Correspondences");
         Collection<Correspondence<MatchableTableColumn, Matchable>> alignments = schemaCorrespondences.get();
         w.write("query_table_id, query_table_name, query_column_id, query_column_name, candidate_table_id, candidate_table_name, candidate_column_id, candidate_column_name, score");
         for(Correspondence<MatchableTableColumn, Matchable> corr : alignments) {
            String c1 = Integer.toString(corr.getFirstRecord().getTableId()) + ", , " + Integer.toString(corr.getFirstRecord().getColumnIndex()) + ", \"" + corr.getFirstRecord().getHeader() + "\", ";
            String c2 = Integer.toString(corr.getSecondRecord().getTableId()) + ", , " + Integer.toString(corr.getSecondRecord().getColumnIndex()) + ", \"" + corr.getSecondRecord().getHeader() + "\", " + String.valueOf(corr.getSimilarityScore()) + "\n"; 
            w.write(c1 + c2);
         }
         w.close();
		//System.err.println("Creating Stitched Union Tables");
		//StitchedUnionTables stitchedUnion = new StitchedUnionTables();
		//Collection<Table> reconstructed = stitchedUnion.create(web.getTables(), web.getRecords(), web.getSchema(), web.getCandidateKeys(), schemaCorrespondences);
		
		//File outFile = new File(resultLocation);
		//outFile.mkdirs();
		//System.err.println("Writing Stitched Union Tables");
		//JsonTableWriter w = new JsonTableWriter();
		//for(Table t : reconstructed) {
		//	w.write(t, new File(outFile, t.getPath()));
		//}
		
		System.err.println("Done.");
	}
	
	private Processable<Correspondence<MatchableTableColumn, Matchable>> runTableMatching(WebTables web) throws Exception {
		TableToTableMatcher matcher = null;
		
    	switch(this.matcher) {
		case CandidateKey:
			matcher = new CandidateKeyBasedMatcher();
			break;
		case Label:
			matcher = new LabelBasedMatcher();
			break;
		case NonTrivialFull:
			matcher = new DeterminantBasedMatcher();
			break;
		case Trivial:
			matcher = new ValueBasedMatcher();
			break;
		case Entity:
			matcher = new EntityLabelBasedMatcher();
			break;
		default:
			break;
    		
    	}
    	
    	DisjointHeaders dh = DisjointHeaders.fromTables(web.getTables().values());
    	Map<String, Set<String>> disjointHeaders = dh.getAllDisjointHeaders();
    	
    	matcher.setWebTables(web);
    	matcher.setMatchingEngine(new MatchingEngine<>());
    	matcher.setDisjointHeaders(disjointHeaders);
		
    	matcher.initialise();
    	matcher.match();
    	
    	Processable<Correspondence<MatchableTableColumn, Matchable>> schemaCorrespondences = matcher.getSchemaCorrespondences();
    	
    	if(prepareGoldStandard) {
    		N2NGoldStandardCreator gsc = new N2NGoldStandardCreator();
    		File evalFile = new File(evaluationLocation);
    		evalFile.mkdirs();
        	gsc.writeInterUnionMapping(new File(evalFile, "inter_union_mapping_generated.tsv"), schemaCorrespondences, web);
        	gsc.writeUnionTablesSchema(new File(evalFile, "union_tables_mapping_generated.tsv"), web.getTables().values());
    	}
    	
    	return schemaCorrespondences;
	}

    //hack: write the web table map to a file
    //    
    public void writeMap (HashMap<String, Integer> tables) {
        System.out.println("writing table map");
        try {
            FileWriter fstream;
            BufferedWriter out;
            fstream = new FileWriter("tables.map");
            out = new BufferedWriter(fstream);
            int count = 0;
            Iterator<Map.Entry<String, Integer>> it = tables.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Integer> pairs = it.next();
                out.write(pairs.getKey() + " : " + pairs.getValue() + "\n");
                count++;
            }
            out.close();
        }catch(Exception e){}
    }
}
