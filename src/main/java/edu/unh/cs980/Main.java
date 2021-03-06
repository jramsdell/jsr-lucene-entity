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
        indexParser.addArgument("--db_name").setDefault("hyperlink.db")
                .help("Directory name to create for Lucene index (default: stuff)");
        indexParser.addArgument("--out").setDefault("stuff")
                .help("Directory name to create for Lucene index (default: stuff)");

        Subparser evaluatorParser = subparsers.addParser("evaluator")
                .setDefault("func", new Exec(Main::evaluate))
                .help("Evaluates F1-measure for TagMe, Spotlight (and optionally, Hyperlink Popularity).");
        evaluatorParser.addArgument("corpus")
                .required(true)
                .help("Location of .cbor file for ground truth (should be lead-paragraphs.cbor");
        evaluatorParser.addArgument("--db")
                .setDefault("")
                .help("Location to hyperlink.db (generated by the index command). " +
                        "If no location is provided, then the Hyperlink Popularity method will not be evaluated.");

        return parser;
    }

    private static void buildCorpusIndex(Namespace params) {
        String corpusFile = params.getString("corpus");
        String db = params.getString("db_name");
        HyperlinkIndexer indexer = new HyperlinkIndexer(db);
        indexer.indexHyperlinks(corpusFile);
    }

    private static void evaluate(Namespace params) {
        String corpusFile = params.getString("corpus");
        String db = params.getString("db");
        KotlinGroundTruth evaluator = new KotlinGroundTruth(corpusFile, db);
        evaluator.evaluateGroundTruths();
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
