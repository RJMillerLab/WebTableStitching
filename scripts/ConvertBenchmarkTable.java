// based on the code of web tables winter project
import java.io.*;
import java.util.*;
import de.uni_mannheim.informatik.dws.winter.utils.Executable;
import de.uni_mannheim.informatik.dws.winter.utils.ProgressReporter;
import de.uni_mannheim.informatik.dws.winter.webtables.Table;
import de.uni_mannheim.informatik.dws.winter.webtables.parsers.CsvTableParser;
import de.uni_mannheim.informatik.dws.winter.webtables.parsers.JsonTableParser;
import de.uni_mannheim.informatik.dws.winter.webtables.writers.CSVTableWriter;
import de.uni_mannheim.informatik.dws.winter.webtables.writers.JsonTableWriter;
import de.uni_mannheim.informatik.dws.winter.webtables.writers.RdfN3TableWriter;
import de.uni_mannheim.informatik.dws.winter.webtables.writers.RdfXmlTableWriter;
import de.uni_mannheim.informatik.dws.winter.webtables.writers.TableWriter;

public class ConvertBenchmarkTable {

    public void listf(String directoryName, ArrayList<String> dirs, ArrayList<String> files) {
        File directory = new File(directoryName);
        File[] fList = directory.listFiles();
        for (File file : fList) {
            if (file.isFile()) {
                files.add(file.getName());
            } 
        }
    }

    public static void main(String[] args) {
        System.out.println("started converting files");
        try {
        ConvertBenchmarkTable cbt = new ConvertBenchmarkTable();
    String inputDirName = "/home/fnargesian/TABLE_UNION_OUTPUT/benchmark-v6/csvfiles";
    String outputDirName = "/home/fnargesian/TABLE_UNION_OUTPUT/benchmark-v6/jsonfiles";
    ArrayList<String> nfiles = new ArrayList<String>();
    ArrayList<String> dirs = new ArrayList<String>();
    cbt.listf(inputDirName, dirs, nfiles); 
    CsvTableParser csvParser = new CsvTableParser();
    JsonTableParser jsonParser = new JsonTableParser();
    JsonTableWriter writer = new JsonTableWriter();
    File outDir = new File(outputDirName);
    outDir.mkdirs();
    File inputDir = new File(inputDirName);
    ProgressReporter p = new ProgressReporter(nfiles.size(), "Converting Files");
    for(String file : nfiles) {
        Table t = null;
        File f = new File(inputDir, file);
        t = csvParser.parseTable(f);

        if(t!=null) {
            writer.write(t, new File(outDir, file));
        }
        p.incrementProgress();
        p.report();
        System.out.println(file);
    }
    }catch(IOException e){
        e.printStackTrace();
    }
}
}
