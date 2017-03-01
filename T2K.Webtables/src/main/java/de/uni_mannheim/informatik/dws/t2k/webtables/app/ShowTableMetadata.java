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
package de.uni_mannheim.informatik.dws.t2k.webtables.app;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.beust.jcommander.Parameter;

import au.com.bytecode.opencsv.CSVWriter;
import de.uni_mannheim.informatik.dws.t2k.utils.ProgressReporter;
import de.uni_mannheim.informatik.dws.t2k.utils.cli.Executable;
import de.uni_mannheim.informatik.dws.t2k.utils.query.Func;
import de.uni_mannheim.informatik.dws.t2k.utils.query.Q;
import de.uni_mannheim.informatik.dws.t2k.webtables.Table;
import de.uni_mannheim.informatik.dws.t2k.webtables.TableColumn;
import de.uni_mannheim.informatik.dws.t2k.webtables.TableContext;
import de.uni_mannheim.informatik.dws.t2k.webtables.TableRow;
import de.uni_mannheim.informatik.dws.t2k.webtables.parsers.JsonTableParser;
import de.uni_mannheim.informatik.dws.t2k.webtables.writers.JsonTableWriter;

/**
 * @author Oliver Lehmberg (oli@dwslab.de)
 *
 */
public class ShowTableMetadata extends Executable {

	@Parameter(names = "-d")
	private boolean showData = false;
	
	@Parameter(names = "-w")
	private int columnWidth = 20;
	
	@Parameter(names = "-keyColumnIndex")
	private Integer keyColumnIndex = null;
	
	@Parameter(names = "-convertValues")
	private boolean convertValues = false;
	
	@Parameter(names = "-update")
	private boolean update = false;
	
	@Parameter(names = "-detectKey")
	private boolean detectKey = false;
	
	@Parameter(names = "-listColumnIds")
	private boolean listColumnIds;
	
	@Parameter(names = "-header")
	private boolean showHeader = false;
	
	@Parameter(names = "-rows")
	private int numRows = 0;
	
	@Parameter(names = "-csv")
	private boolean createCSV = false;
	
	public static void main(String[] args) throws IOException {
		ShowTableMetadata s = new ShowTableMetadata();
		
		if(s.parseCommandLine(ShowTableMetadata.class, args) && s.getParams()!=null) {
			
			s.run();
		}
	}
	
	public void run() throws IOException {
		
		JsonTableParser p = new JsonTableParser();
		JsonTableWriter w = new JsonTableWriter();
		p.setConvertValues(convertValues | detectKey);
		
		String[] files = getParams().toArray(new String[getParams().size()]);
		
		File dir = null;
		if(files.length==1) {
			dir = new File(files[0]);
			if(dir.isDirectory()) {
				files = dir.list();
			} else {
				dir = null;
			}
		}
		
		ProgressReporter prg = new ProgressReporter(files.length, "Processing Tables");
		
		CSVWriter csvW = null;
		if(createCSV) {
			csvW = new CSVWriter(new OutputStreamWriter(System.out));
		}
		
		for(String s : files) {
			
			Table t = null;
			
			File f = new File(s);
			if(dir!=null) {
				f = new File(dir,s);
			}
			
			t = p.parseTable(f);
			
			if(createCSV) {
				csvW.writeNext(new String[] {
						s,
						Integer.toString(t.getRows().size()),
						Integer.toString(t.getColumns().size()),
						t.getContext().getUrl()
				});
			} else if(listColumnIds) {
				for(TableColumn c : t.getColumns()) {
					if(!showHeader) {
						System.out.println(c.getIdentifier());
					} else {
						System.out.println(c.toString());
					}
				}
			} else {
			
				if(keyColumnIndex!=null) {
					t.setKeyIndex(keyColumnIndex);
				}
				
				if(update) {
					//w.write(t, new File(s));
					w.write(t, f);
				} else {
				
					TableContext ctx = t.getContext();
					
					System.out.println(String.format("*** Table %s ***", s));
					System.out.println(String.format("* URL: %s", ctx.getUrl()));
					System.out.println(String.format("* Title: %s", ctx.getPageTitle()));
					System.out.println(String.format("* Heading: %s", ctx.getTableTitle()));
					System.out.println(String.format("* # Columns: %d", t.getColumns().size()));
					System.out.println(String.format("* # Rows: %d", t.getRows().size()));
					System.out.println(String.format("* Created from %d original tables", getOriginalTables(t).size()));
					System.out.println(String.format("* Entity-Label Column: %s", t.getKey()==null ? "?" : t.getKey().getHeader()));
					
					if(detectKey) {
						t.identifyKey(0.3,true);
						System.out.println(String.format("* Detected Entity-Label Column: %s", t.getKey()==null ? "?" : t.getKey().getHeader()));
					}
					
		
					
					if(showData) {
						System.out.println(t.getSchema().format(columnWidth));
						System.out.println(t.getSchema().formatDataTypes(columnWidth));
						
						int maxRows = Math.min(numRows, t.getRows().size());
						
						if(maxRows==0) {
							maxRows = t.getRows().size();
						}
						
						for(int i = 0; i < maxRows; i++) {
//						for(TableRow r : t.getRows()) {
							TableRow r = t.getRows().get(i);
							System.out.println(r.format(columnWidth));
						}
					} else {
						System.out.println(StringUtils.join(Q.project(t.getColumns(), 
								new Func<String, TableColumn>() {
		
									@Override
									public String invoke(TableColumn in) {
										return String.format("%s (%s)", in.getHeader(), in.getDataType());
									}}
								), ", "));
					}
				}
			
				prg.incrementProgress();
				prg.report();
			}
		}
		
		if(createCSV) {
			csvW.close();
		}
		
	}
	
	private Set<String> getOriginalTables(Table t) {
		
		Set<String> tbls = new HashSet<>();
		
		for(TableColumn c : t.getColumns()) {
			for(String prov : c.getProvenance()) {
				
				tbls.add(prov.split(";")[0]);
				
			}
		}
		
		return tbls;
	}
}
