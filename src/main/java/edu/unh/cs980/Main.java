package edu.unh.cs980;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.*;
import java.util.function.Consumer;

public class Main {

    // Used as a wrapper around a static method: will call method and pass
    // argument parser's parameters to it
    private static class Exec {
        private Consumer<Namespace> func;

        Exec(Consumer<Namespace> funcArg) {
            func = funcArg;
        }

        void run(Namespace params) {
            func.accept(params);
        }
    }

    public static ArgumentParser createArgParser() {
        ArgumentParser parser = ArgumentParsers.newFor("program").build();
        Subparsers subparsers = parser.addSubparsers(); // Subparsers is used to
        // create subcommands

        Subparser indexParser = subparsers.addParser("index")
                .setDefault("func", new Exec(Main::buildCorpusIndex)).help("Indexes paragraph corpus using Lucene.");
        indexParser.addArgument("corpus").required(true).help("Location to paragraph corpus file (.cbor)");
        indexParser.addArgument("--db_name").setDefault("")
                .help("Directory name to create for Lucene index (default: stuff)");
        indexParser.addArgument("--out").setDefault("stuff")
                .help("Directory name to create for Lucene index (default: stuff)");


//		// Ranklib Trainer
//		Subparser ranklibTrainerParser = subparsers.addParser("ranklib_trainer")
//				.setDefault("func", new Exec(Main::runRanklibTrainer))
//				.help("Scores using methods and writes features to a RankLib compatible file for use with training.");
//
//		ranklibTrainerParser.addArgument("method")
//				.help("The type of method to use when training (see readme).")
//				.choices("entity_similarity", "average_query", "split_sections", "mixtures", "combined",
//						"lm_mercer", "lm_dirichlet");
//		ranklibTrainerParser.addArgument("index").help("Location of the Lucene index directory");
//		ranklibTrainerParser.addArgument("query").help("Location of query file (.cbor)");
//		ranklibTrainerParser.addArgument("qrel").help("Locations of matching qrel file.");
//		ranklibTrainerParser.addArgument("--out")
//				.setDefault("ranklib_features.txt")
//				.help("Output name for the RankLib compatible feature file.");
//		ranklibTrainerParser.addArgument("--graph_database")
//				.setDefault("")
//				.help("(only used for mixtures method): Location of graph_database.db file.");


        return parser;
    }

    private static void buildCorpusIndex(Namespace params) {
        String indexLocation = params.getString("out");
        String corpusFile = params.getString("corpus");
        String spotlight_location = params.getString("spotlight_folder");
        String db = params.getString("db_name");
        KotCborParser.getStuff(corpusFile, "mydb.db");
    }


    // Main class for project
    public static void main(String[] args) {
        System.setProperty("file.encoding", "UTF-8");
        ArgumentParser parser = createArgParser();

        try {
            // This parses the arguments (based on createArgParser) and returns
            // the results
            Namespace params = parser.parseArgs(args);

            // We store the function that handles using these parameters in the
            // "func" field
            // In this example, we retrieve the parameter and cast it as Exec,
            // which is used to run the method reference
            // That was passed to it when the Exec was created.
            ((Exec) params.get("func")).run(params);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
    }

}
