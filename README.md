# jsr-lucene-entity
___
## Installation Instructions
A precompiled jar file can be found in bin/program.jar

You may also compile the source code by entering the following command (or by running ./compile.sh):

```bash
mvn clean compile assembly:single
```

This will create an executable jar file in the target/ directory.
___
## Program Commands
The program is divided into the following subcommands:


#### Indexer (index)
This is responsible for parsing the very large allButBenchmark corpus. It extracts anchor text and the entity links it refers to and builds a database from this information (default is hyperlink.db). This step is **optional** as it is only required for evaluating my entity linking method (the Spotlight and TagMe entity linkers do not require it). The corpus file is located in the server at:

```bash
/trec_data/unprocessedAllButBenchmark/unprocessedAllButBenchmark.cbor
```
#### Usage
```bash
program.jar index corpus [--spotlight_folder ""] [--out "index"]
```
Where:

**corpus**: Is the unprocessedAllButBenchmark.cbor file to extract the links from.

**--db_name**: Is the name of the database to store the indexed data in. Default: "hyperlink.db"

___

#### Evaluator (evaluator)
This command evaluates entity linkers with respect to the F1-measure. It uses the lead-paragraphs.cbor file to create a ground truth. The text of each of these paragraphs is linked and the results are compared to the ground truth entities. 
The methods that are evaluated are: TagMe, Spotlight, and (optionally) Hyperlink Popularity

To make your life easier, the database file (hyperlink.db) and the ground set paragraphs (lead-paragraphs.cbor) are already on the server at:
```bash
/trec_data/jordan/hyperlink.db
/trec_data/lead-paragraphs.cbor
```
#### Usage
```bash
program.jar evaluator corpus [--db ""]
```
Where:

**corpus**: Is the lead-paragraphs.cbor file to derive the ground truth from.

**--db_name**: Is the hyperlink.db used in my hyperlink entity linking method. If you do not include this option, then my method is skipped and only TagMe and Spotlight are evaluated.
