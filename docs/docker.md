# How to build and run qubership-inventory-tool CLI in Docker

## Run a single component on CI

Run a tool on a single component and generate inventory tool data model for it.

Appropriate branch of target component must be checked out from source repository before running the tool
and mounted into running container. 

It is highly recommended that target component has `inventory.json` file.
They are used to assign proper abbreviation and domain (and also contain other information for inventory tool).
If both inventory files are missing, mock domain and abbreviation will be generated.

**Command line:**  
`docker run itool ci-exec [params]`

**Required mount points:**
- `/var/input` (RO) -- a directory where appropriate branch of target component is checked out
- `/var/output` (RW) -- a directory where results will be stored

**Required command-line parameters:**
- `--repository=https://git.your.host/something/something.git` -- repository address of target component
- `--componentName=<component-name>` -- component name from builder

Provided component name is stored into meta-info of generated graph and later may appear in fallout report.

**Optional command-line parameters:**
- `--componentVersion=<version>` -- component version from builder
- `--rb=<branch>` -- a branch of target component being processed (Not needed if `git.info` file is provided)
- `--inputDirectory=<dir>` -- a directory where target component is checked out. **Default:** `/var/input`
- `--outputDirectory=<dir>` -- a directory to store data model of target component to. **Default:** `/var/output`
- `--outputFile=<filename>` -- file name to store data model of target component to.
- `--dumpResultsBy=<strategy>` -- a strategy to generate output file name in case it is not provided.  
Possible values: `hash`, `id`, `repo`.  
Default value: `hash` -- in this case, file name is a hash of target component repo address.


## Assemble component graphs into application

Combine data models of multiple components into a single application data model.

**Command line:**  
`docker run itool ci-assembly [params]`

**Required mount points:**
- `/var/input` (RO) -- a directory where all source data models are stored
- `/var/output` (RW) -- a directory where assembly results will be stored

**Required command-line parameters:**
- `--appName=<application-name>` -- name of the application being built.

Provided application name is stored into meta-info of generated graph and later may appear in fallout report.

**Optional command-line parameters:**
- `--appVersion=<version>` -- application version from builder
- `--inputDirectory=<dir>` -- a directory where all needed data models are stored. **Default:** `/var/input`
- `--outputDirectory=<dir>` -- a directory where assembly results will be stored. **Default:** `/var/output`
- `--outputFile=<filename>` -- file name to store assembled data model to. **Default:** `assembly.result.json`


## Obfuscate a graph

Obfuscate data model of single or multiple components by removing possibly sensitive data.

**Command line:**  
`docker run itool ci-obfuscate [params]`

**Required mount points:**
- `/var/input` (RO) -- a directory where input file resides
- `/var/output` (RW) -- a directory where results will be stored. **May be the same as input directory.**

**Required command-line parameters:**
- `--inputFile=<filename>` -- name of a file containing input data model.
- `--outputFile=<filename>` -- file name to store obfuscated data model to.

**Optional command-line parameters:**
- `--inputDirectory=<dir>` -- a directory where input file is located. **Default:** `/var/input`
- `--outputDirectory=<dir>` -- a directory where results will be stored. **Default:** `/var/output`

