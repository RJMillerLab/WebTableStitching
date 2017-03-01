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
package de.uni_mannheim.informatik.dws.t2k.webtables.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import de.uni_mannheim.informatik.dws.t2k.datatypes.DataType;
import de.uni_mannheim.informatik.dws.t2k.utils.data.string.StringCache;
import de.uni_mannheim.informatik.dws.t2k.webtables.Table;
import de.uni_mannheim.informatik.dws.t2k.webtables.TableMapping;
import de.uni_mannheim.informatik.dws.t2k.webtables.TableRow;
import de.uni_mannheim.informatik.dws.t2k.webtables.lod.LodTableColumn;
import de.uni_mannheim.informatik.dws.t2k.webtables.lod.LodTableRow;

public class LodCsvTableParser {

	public static String delimiter = "\",\"";
	
	private boolean useStringCache = true;
	
	public void setUseStringCache(boolean use) {
		useStringCache = use;
	}
	
    public Table parseTable(File file) {
        Reader r = null;
        Table t = null;
        try {
            if (file.getName().endsWith(".gz")) {
                GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
                r = new InputStreamReader(gzip, "UTF-8");
            } else {
                r = new InputStreamReader(new FileInputStream(file), "UTF-8");
            }
            
            t = parseTable(r, file.getName());
            
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return t;
    }
    
    public Table parseTable(Reader reader, String fileName) throws IOException {        
        // create new table
        Table table = new Table();
        // take care of the header of the table
        table.setPath(fileName);

        // table may contain additional annotations (currently not used for DBpedia tables)
        TableMapping tm = new TableMapping();
        
        try {
            String[] columnNames;
            String[] columntypes;
            String[] columnURIs;

            BufferedReader in = new BufferedReader(reader);

            // read the property names
            String fileLine = in.readLine();
            columnNames = fileLine.split(delimiter);

            // skip annotations
            boolean isMetaData = columnNames[0].startsWith("#");
            // if the current line starts with #, check for valid annotations
            while(isMetaData) {
                isMetaData = false;
                
                // check all valid annotations
                for(String s : TableMapping.VALID_ANNOTATIONS) {
                    if(columnNames[0].startsWith(s)) {
                        isMetaData = true;
                        break;
                    }
                }
                
                // if the current line is an annotation, read the next line and start over
                if(isMetaData) {
                	tm.parseMetadata(fileLine);
                	
                    fileLine = in.readLine();
                    columnNames = fileLine.split(delimiter);
                    isMetaData = columnNames[0].startsWith("#");
                }
            }
            
            // read the property URIs
            columnURIs = in.readLine().split(delimiter);

            // read the datatypes
            fileLine = in.readLine();
            columntypes = fileLine.split(delimiter);

            // process all properties (=columns)
            int i = 0;
            for (String columnName : columnNames) {

                // replace trailing " for the last column
                columntypes[i] = columntypes[i].replace("\"", "");
                columnURIs[i] = columnURIs[i].replace("\"", "");
                columnName = columnName.replace("\"", "");

                // create the column
                LodTableColumn c = new LodTableColumn(i, table);
                c.setHeader(columnName);
                
                if(columnName.endsWith("_label")) {
                	c.setReferenceLabel(true);
                }
                c.setUri(columnURIs[i]);

                // set the type if it's a primitive
                //TODO what about other primitive types?
                String datatype = columntypes[i];
                switch (datatype) {
                case "XMLSchema#date":
                case "XMLSchema#gYear":
                    c.setDataType(DataType.date);
                    break;
                case "XMLSchema#double":
                case "XMLSchema#float":
                case "XMLSchema#nonNegativeInteger":
                case "XMLSchema#positiveInteger":
                case "XMLSchema#integer":    
                case "XMLSchema#negativeInteger": 
                    c.setDataType(DataType.numeric);
                    break;
                case "XMLSchema#string":
                case "rdf-schema#Literal":
                    c.setDataType(DataType.string);                    
                    break;
                default:                    
                    c.setDataType(DataType.unknown);
                }

                // add the column to the table
                table.addColumn(c);
                i++;
            }

            // skip the last header
            fileLine = in.readLine();
            
            int row = 4;
            Object[] values;
            
            // read the table rows
            while ((fileLine = in.readLine()) != null) {
                
            	// handle the column splitting
            	fileLine = fileLine.substring(1, fileLine.length() - 1);
                String[] stringValues = fileLine.split(delimiter);  
                
                // create the value array
                values = new Object[columnNames.length];
                
                // transfer values
            	for (int j = 0; j < stringValues.length; j++) {
					if(stringValues[j].equalsIgnoreCase("NULL")) {
						values[j] = null;
					} else {
						if(useStringCache) {
							values[j] = StringCache.get(stringValues[j]);
						} else {
							values[j] = stringValues[j];
						}
					}
				}
                
            	// create the row, set the values and add it to the table
                TableRow r = new LodTableRow(row++, table);
                r.set(values);
                table.addRow(r);
            }
            
        } catch(Exception ex) {
        	ex.printStackTrace();
        }
        
        reader.close();
        
        table.inferSchemaAndConvertValues();
        
        return table;
    }
	
    public static void endLoadData() {
    	StringCache.clear();
    }
}
