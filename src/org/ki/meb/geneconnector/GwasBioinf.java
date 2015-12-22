package org.ki.meb.geneconnector;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.ConfigurationNode;
import org.apache.derby.impl.sql.execute.OnceResultSet;
import org.apache.ibatis.jdbc.SQL;
import org.ki.meb.common.ApplicationException;
import org.ki.meb.common.ParallelWorker;
import org.ki.meb.geneconnector.GwasBioinfCustomFormatter.InputOutputType;

//import getl.proc.Flow;

public class GwasBioinf //extends ParallelWorker
{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private CommandLine commandLine;
	
	private static Options clOptions = new Options();
	
	private File settingInputFolder, settingOutputFolder, settingConfigFile;
	private GwasBioinfDataCache dataCache;
	private FilenameFilter filterExcelXlsx, filterExcelCSV;
	
	static
	{
		//clOptions.addOption(OptionBuilder.create(TextMap.regtest)); //inactivated as default
		clOptions.addOption(TextMap.help,false,"Print usage help.");
		clOptions.addOption(OptionBuilder.withArgName("file path").withDescription("Config file").hasArg().create(TextMap.config));
		clOptions.addOption(TextMap.init,false,"Initiate the database content from input files");
		//clOptions.addOption(TextMap.refresh,false,"Refresh the database content from input files");
		clOptions.addOption(TextMap.operate,false,"Perform operation specifics");
		clOptions.addOption(TextMap.get,false,"Get the database content as exported output");
		
		
		
		clOptions.addOption(OptionBuilder.withArgName("file path").withDescription("Path to the folder of the input files used for initiation").hasArg().create("ipath"));
		clOptions.addOption(OptionBuilder.withArgName("file path").withDescription("Database function jar (special case)").hasArg().create("dbjar"));
		
		
	}

	public GwasBioinf()
	{
		filterExcelXlsx= new FilenameFilter() 
		{
			@Override
			public boolean accept(File dir, String name) 
			{
				return name.toLowerCase().matches("^.+\\.xlsx$");
			}
		};
		
		filterExcelCSV= new FilenameFilter() 
		{
			@Override
			public boolean accept(File dir, String name) 
			{
				return name.toLowerCase().matches("^.+\\.csv$");
			}
		};
	}

	private void init() throws ConfigurationException
	{
		XMLConfiguration config = new XMLConfiguration(settingConfigFile);
		ConfigurationNode rootNode = config.getRootNode();
		//setDBConnectionString((String)((ConfigurationNode)rootNode.getChildren(TextMap.database_connectionstring).get(0)).getValue());
		//setResourceFolder((String)((ConfigurationNode)rootNode.getChildren(TextMap.resourcefolderpath).get(0)).getValue());
		settingInputFolder = new File((String)((ConfigurationNode)rootNode.getChildren(TextMap.inputfolderpath).get(0)).getValue());
		settingOutputFolder = new File((String)((ConfigurationNode)rootNode.getChildren(TextMap.outputfolderpath).get(0)).getValue());
		dataCache=new GwasBioinfDataCache().setRefreshExistingTables(commandLine.hasOption(TextMap.refresh));
		if(commandLine.hasOption("dbjar"))
			dataCache.setDBJarFile(new File(commandLine.getOptionValue("dbjar")));
		
	}
	
	public static CommandLine constructCommandLine(String[] args) throws ParseException
	{
		CommandLineParser parser = new org.apache.commons.cli.GnuParser();
		CommandLine commandLine = parser.parse(clOptions, args);
		return commandLine;
	}
	
	
	public static void main(String[] args) throws Exception
	{
		//System.out.println("TEST>"+Utils.stringSeparateFixedSpacingRight("abcdefg", ",", 3));
		new GwasBioinf().setCommandLine(constructCommandLine(args)).runCommands();
	}
	
	//always to standard output
	private void printHelp()
	{
		System.out.println("Report Generator Command Line Application");
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("ReportGenerator", "", clOptions, "", true);
	}
	
	public GwasBioinf setCommandLine(CommandLine nCommandLine)
	{
		commandLine=nCommandLine;
		return this;
	}
	
	//Camel version -test
	/*
	private void initDataFromFiles_camel() throws Exception
	{
		CamelContext routingEngineContext = new DefaultCamelContext();
		ConnectionFactory conFactory = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
		routingEngineContext.addComponent("jmscomponent", JmsComponent.jmsComponentAutoAcknowledge(conFactory));
		
		RouteBuilder routeBuilder = new RouteBuilder() 
		{
			
			@Override
			public void configure() throws Exception 
			{
				from("jmscomponent:queue:test.queue").to("file://test");
			}
		};
		
		routingEngineContext.addRoutes(routeBuilder);
		
		
		ProducerTemplate template = routingEngineContext.createProducerTemplate();
		
		routingEngineContext.start();
		
		for (int i = 0; i < 10; i++) 
		{
            template.sendBody("jmscomponent:queue:test.queue", "Hello Camel! Test Message: " + i);
        }
		
		Thread.sleep(1000);
		if(!routingEngineContext.getStatus().isStoppable())
		{
			throw new Exception("Can't stop the routing context!");
		}
		routingEngineContext.stop();
	}
	*/
	
	//POI version
	private void initDataFromFiles() throws ApplicationException, Exception
	{
		GwasBioinfCustomFormatter inputReader = new GwasBioinfCustomFormatter().setDataCache(dataCache).setOutputType(InputOutputType.DATACACHE);
		//import all files in input
		File[] inputFilesCsv = settingInputFolder.listFiles(filterExcelCSV);
		for(int iFile=0; iFile<inputFilesCsv.length; iFile++)
		{
			inputReader.setInputType(InputOutputType.CSV).setInputFile(inputFilesCsv[iFile]).read();
		}
		
		File[] inputFilesXlsx = settingInputFolder.listFiles(filterExcelXlsx);
		for(int iFile=0; iFile<inputFilesXlsx.length; iFile++)
		{
			inputReader.setInputType(InputOutputType.EXCEL).setInputFile(inputFilesXlsx[iFile]).read();
		}
		
		
	}
	
	private void getData() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, ApplicationException
	{

		//TODO
	}
	/*
	private void performTavernaWorkflow() throws ReaderException, IOException
	{
		WorkflowBundleIO io = new WorkflowBundleIO();
		WorkflowBundle ro = io.readBundle(new File("workflow.t2flow"), null);
		ro.getMainWorkflow();
	}
	*/
	private GwasBioinf runCommands() throws Exception
	{
		if(commandLine.hasOption(TextMap.help))
		{
			printHelp();
			return this;
		}
		
		if(commandLine.hasOption(TextMap.config))
		{
			settingConfigFile = new File(commandLine.getOptionValue(TextMap.config));
		}
		else
		{
			settingConfigFile = new File("config.xml");
		}
		
		init();
		
		dataCache.createCacheConnection();
		
		if(commandLine.hasOption(TextMap.init))
		{
			//initDataFromFiles_camel();
			initDataFromFiles();
		}
		
		if(commandLine.hasOption(TextMap.operate))
		{
			operate();
		}
		
		if(commandLine.hasOption(TextMap.get))
		{
			getData();
		}
		
		dataCache.shutdownCacheConnection();
		
		return this;
	}
	
	
	private void operate() throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException, ApplicationException
	{
		
		
		
		
		//String schemaName = "APP"; 
		String schemaName = "PUBLIC";
		
		StringBuilder qSB = new StringBuilder();
		String q;
		
		
		//preparations
		dataCache.index(schemaName+".gencode_master", "f1");
		dataCache.index(schemaName+".gencode_master", "f4");
		dataCache.index(schemaName+".gencode_master", "f5");
		dataCache.index(schemaName+".gencode_master", "f7");
		dataCache.index(schemaName+".gencode_master", "genename");
		
		dataCache.index(schemaName+".MDD2CLUMPRAW", "RANK");
		dataCache.index(schemaName+".MDD2CLUMPRAW", "six1");
		dataCache.index(schemaName+".MDD2CLUMPRAW", "six2");
		dataCache.index(schemaName+".MDD2CLUMPRAW", "r1");
		dataCache.index(schemaName+".MDD2CLUMPRAW", "r2");
		
		//operations
		SQL candidateInner = new SQL()
		{
			{
				SELECT("RANK AS RANK_MDD2");
				SELECT("SNPID");
				SELECT("A1A2");
				SELECT("OR");
				SELECT("P");
				SELECT("UCSC");
				SELECT("hg19chrc");
				SELECT("CHR");
				SELECT("BP");
				SELECT("ONE1");
				SELECT("ONE2");
				SELECT("SIX1");
				SELECT("SIX2");
				SELECT("SE");
				SELECT("FRQ_A_44194");
				SELECT("FRQ_U_110741");
				SELECT("INFO");
				SELECT("NGT");
				SELECT(dataCache.scriptSeparateFixedSpacingRight(dataCache.scriptDoubleToVarchar("six1"),",", 3)+"||'-'||"+dataCache.scriptSeparateFixedSpacingRight(dataCache.scriptDoubleToVarchar("six2"),",", 3)+" AS ll");
				SELECT("hg19chrc AS r0");
				SELECT("six1 AS r1");
				SELECT("six2 AS r2");
				SELECT("hg19chrc||':'||"+dataCache.scriptSeparateFixedSpacingRight(dataCache.scriptDoubleToVarchar("six1"),",", 3)+"||'-'||"+dataCache.scriptSeparateFixedSpacingRight(dataCache.scriptDoubleToVarchar("six2"),",", 3)+" AS cc");
				SELECT("'=HYPERLINK(\"http://genome.ucsc.edu/cgi-bin/hgTracks?&org=Human&db=hg19&position='||hg19chrc||'%3A'||"+dataCache.scriptDoubleToVarchar("six1")+"||'-'||"+dataCache.scriptDoubleToVarchar("six2")+"||'\",\"ucsc\")' AS nucsc");
				
				FROM(schemaName+".MDD2CLUMPRAW");
				
				WHERE("P>0 AND P<1e-1");
				ORDER_BY("P");
			}
		};
		
		SQL candidateOuter = new SQL()
		{
			{
				SELECT("ROWNUM() AS RANK, SUB.*");
				FROM("("+candidateInner+") AS SUB");
			}
		};
		
		q=candidateOuter.toString();
		dataCache.view("candidate", q).commit();
		
		//*====== genes for bioinformatics ======;
		//*=== Genes: GENCODE v17 genes in bin, expand by 20kb;
		
		q=new SQL()
		{
			{
				SELECT("gencode_master.*");
				SELECT("(f4-20000) AS e4");
				SELECT("(f5+20000) AS e5");
				FROM(schemaName+".gencode_master");
			}
		}.toString();
		dataCache.view("t2", q).commit();
		
		q=new SQL()
		{
			{
				SELECT("*");
				FROM(schemaName+".candidate");
				INNER_JOIN(schemaName+".t2 ON (r0=f1 AND ((r1<=e4 AND e4<=r2) OR (r1<=e5 AND e5<=r2) OR (e4<=r1 AND r1<=e5) OR (e4<=r2 AND r2<=e5)))");
				
			}
		}.toString();
		dataCache.view("t3", q).commit();
		
		q=new SQL()
		{
			{
				SELECT("RANK");
				SELECT("p");
				SELECT("hg19chrc");
				SELECT("six1");
				SELECT("six2");
				SELECT("f1 AS g0");
				SELECT("f4 AS g1");
				SELECT("f5 AS g2");
				SELECT("f7 AS gstr");
				SELECT("gid AS ensgene");
				FROM(schemaName+".t3");
			}
		}.toString();
		dataCache.view("allgenes20kb", q).commit();
		
		//*====== PC genes & distance ======;
		//* expand by 10mb;
		
		q=new SQL()
		{
			{
				SELECT("gencode_master.*");
				SELECT("(f4-10e6) AS e4");
				SELECT("(f5+10e6) AS e5");
				FROM(schemaName+".gencode_master");
				WHERE("ttype='protein_coding'");
			}
		}.toString();
		dataCache.view("r1", q).commit();
		
		//* join;
		q=new SQL()
		{
			{
				SELECT("ROWNUM() AS _id");
				SELECT("RANK");
				SELECT("p");
				SELECT("hg19chrc");
				SELECT("six1");
				SELECT("six2");
				SELECT("r0");
				SELECT("r1");
				SELECT("r2");
				SELECT("CC");
				SELECT("NUCSC");
				SELECT("GENENAME");
				SELECT("F1");
				SELECT("F4");
				SELECT("F5");
				SELECT("F7");
				SELECT("GID");
				SELECT("TTYPE");
				SELECT("STATUS");
				SELECT("HUGOPRODUCT");
				SELECT("HUGOALIAS");
				FROM(schemaName+".candidate");
				LEFT_OUTER_JOIN(schemaName+".r1 ON (r0=f1 AND ((r1<=e4 AND e4<=r2) OR (r1<=e5 AND e5<=r2) OR (e4<=r1 AND r1<=e5) OR (e4<=r2 AND r2<=e5)))");
			}
		}.toString();
		dataCache.view("r2", q).commit();
		
		//* classify;
		q=new SQL()
		{
			{
				SELECT("r2.*");
				SELECT("( CASE WHEN ((r1<=f4) AND (f4<=r2)) OR ((r1<=f5) AND (f5<=r2)) OR ((f4<=r1) AND (r1<=f5)) OR ((r2<=f5) AND (f4<=r2)) THEN 0 WHEN f4 IS NULL OR f5 IS NULL THEN 9e9 ELSE NUM_MIN_INTEGER(NUM_MIN_INTEGER(ABS(r1-f4),ABS(r2-f4)),NUM_MIN_INTEGER(ABS(r1-f5),ABS(r2-f5))) END) dist");
				FROM(schemaName+".r2");
				ORDER_BY("RANK, dist");
			}
		}.toString();
		dataCache.table("r3", q).commit();
		
		
		//TODO
		/*
		 * Check if this is correct
		 */
		q=new SQL()
		{
			{
				SELECT("RANK");
				SELECT("p");
				SELECT("hg19chrc");
				SELECT("six1");
				SELECT("six2");
				SELECT("dist AS r2"); //* for column position;
				SELECT("GENENAME");
				SELECT("F1");
				SELECT("F4");
				SELECT("F5");
				SELECT("F7");
				SELECT("GID");
				SELECT("TTYPE");
				SELECT("STATUS");
				SELECT("HUGOPRODUCT");
				SELECT("HUGOALIAS");
				SELECT("dist AS distance");
				FROM(schemaName+".r3");
				WHERE("dist<100000");
				ORDER_BY("RANK,p");
			}
		}.toString();
		dataCache.table("genesPCnear", q).commit();
		
		//*=== nhgri gwas;
		q=new SQL()
		{
			{
				SELECT("RANK");
				SELECT("p");
				SELECT("hg19chrc");
				SELECT("six1");
				SELECT("six2");
				SELECT("r0");
				SELECT("r1");
				SELECT("r2");
				//SELECT("bb.*");
				FROM(schemaName+".candidate aa");
				INNER_JOIN(schemaName+".nhgri_gwas bb ON aa.r0=bb.hg19chrom AND r1<=bb.bp AND bb.bp<=r2");
			}
		}.toString();
		dataCache.table("link_nhgri", q).commit();
		
		//*=== omim;
		q=new SQL()
		{
			{
				SELECT("RANK, p, aa.geneName, hugoproduct, type");
				FROM(schemaName+".genesPCnear aa");
				INNER_JOIN(schemaName+".omim bb ON aa.geneName=bb.geneName");
			}
		}.toString();
		dataCache.table("link_omim", q).commit();
		
		//*=== aut_loci_hg19 ;
		q=new SQL()
		{
			{
				SELECT("RANK, p, aa.geneName, hugoproduct");
				FROM(schemaName+".genesPCnear aa");
				INNER_JOIN(schemaName+".aut_loci_hg19 bb ON aa.geneName=bb.geneName AND aa.geneName IS NOT NULL AND aa.geneName!='' AND bb.geneName IS NOT NULL AND bb.geneName!=''");
			}
		}.toString();
		dataCache.table("link_aut", q).commit();
		
		//*=== id/dev delay ;
		q=new SQL()
		{
			{
				SELECT("RANK, p, aa.geneName, hugoproduct");
				FROM(schemaName+".genesPCnear aa");
				INNER_JOIN(schemaName+".id_devdelay bb ON aa.geneName=bb.geneName AND aa.geneName IS NOT NULL AND aa.geneName!='' AND bb.geneName IS NOT NULL AND bb.geneName!=''");
			}
		}.toString();
		dataCache.table("link_iddd", q).commit();
		
		//*=== jax;
		q=new SQL()
		{
			{
				SELECT("rank, p, aa.geneName, hugoproduct, musName, KOphenotype");
				FROM(schemaName+".genesPCnear aa");
				INNER_JOIN(schemaName+".mouse bb ON aa.geneName=bb.geneName AND aa.geneName IS NOT NULL AND aa.geneName!='' AND bb.geneName IS NOT NULL AND bb.geneName!=''");
			}
		}.toString();
		dataCache.table("link_jax", q).commit();
		
		//*=== psych CNVs;
		q=new SQL()
		{
			{
				SELECT("rank, p, hg19chrc, six1, six2, bb.*");
				FROM(schemaName+".candidate aa");
				INNER_JOIN(schemaName+".psych_cnv_hg19 bb ON aa.r0=bb.c0 AND "+dataCache.scriptTwoSegmentOverlapCondition("r1", "r2", "c1", "c2"));
			}
		}.toString();
		dataCache.table("link_cnv", q).commit();
		
		//*=== g1000 sv;
		q=new SQL()
		{
			{
				SELECT("rank, p, bb.hg19chrc, six1, six2, bp1, BP2, SVTYPE, EURAF, ASIAF, AFRAF");
				SELECT("(NUM_MIN_INTEGER(six2,bp2)-NUM_MAX_INTEGER(six1,bp1))/(NUM_MAX_INTEGER(six2,bp2)-NUM_MIN_INTEGER(six1,bp1)) AS recipoverlap");
				SELECT("'=HYPERLINK(\"http://genome.ucsc.edu/cgi-bin/hgTracks?&org=Human&db=hg19&position='||bb.hg19chrc||'%3A'||"+dataCache.scriptDoubleToVarchar("bp1")+"||'-'||"+dataCache.scriptDoubleToVarchar("bp2")+"||'\",\"g1000sv\")' AS g1000sv");
				FROM(schemaName+".candidate aa");
				INNER_JOIN(schemaName+".g1000sv bb ON euraf>0.01 AND aa.r0=bb.hg19chrc AND "+dataCache.scriptTwoSegmentOverlapCondition("r1", "r2", "bp1", "bp2"));
			}
		}.toString();
		dataCache.table("link_g1000sv", q).commit();
		
		//*=== GPCRs;
		q=new SQL()
		{
			{
				SELECT("RANK");
				SELECT("p");
				SELECT("hg19chrc");
				SELECT("six1");
				SELECT("six2");
				SELECT("r2");
				SELECT("aa.GENENAME");
				SELECT("FAMILY, HG19CHROM, BP1, BP2, MAPTOTAL, bb.HUGOPRODUCT");
				FROM(schemaName+".genesPCnear aa");
				INNER_JOIN(schemaName+".gproteincoupledreceptors bb ON aa.geneName=bb.geneName AND aa.geneName IS NOT NULL AND aa.geneName!='' AND bb.geneName IS NOT NULL AND bb.geneName!=''");
			}
		}.toString();
		dataCache.table("link_gpcr", q).commit();
		
		
		//*=== psych linkage meta;
		q=new SQL()
		{
			{
				SELECT("rank, p, hg19chrc, six1, six2, bp1, bp2");
				SELECT("(NUM_MIN_INTEGER(six2,bp2)-NUM_MAX_INTEGER(six1,bp1))/(NUM_MAX_INTEGER(six2,bp2)-NUM_MIN_INTEGER(six1,bp1)) AS recipoverlap");
				FROM(schemaName+".candidate aa");
				INNER_JOIN(schemaName+".psych_linkage bb ON bb.study='GWL' AND bb.type='MetaAnal' AND aa.r0=hg19chrc AND "+dataCache.scriptTwoSegmentOverlapCondition("r1", "r2", "bp1", "bp2"));
			//six1 IS NOT NULL AND six2 IS NOT NULL AND bp1 IS NOT NULL AND bp2 IS NOT NULL AND
			}
		}.toString();
		dataCache.table("link_psychlinkage", q).commit();
		
	}
	
}
