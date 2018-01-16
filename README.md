# remedy-tsi-bulkingestion-script
Script for ingestion of historical Remedy Incidents and Change tickets into Truesight Intelligence as events.

## Description.

This Script ingests historical Remedy incidents and change tickets into TSI. Based on the configuration present in the template(see in dist), this script reads the Remedy Incident & Change Tickets and after converting into a TSI event (based on definitions in  `eventDefinition`), it ingests the events to TSI.


## Prerequisite 
```
1. Java Sdk 1.8
2. Maven*
3. Remedy Api Java sdk*
(*Only required if you want to contribute and build the code)
```

## How to run ?
```
1. There is a pre-built jar shared at location dist. Please navigate to this directory.
$ cd dist
2. The incidentTemplate.json & changeTemplate.Json are the user input files.
  Change the incidentTemplate.json/changeTemplate.json configuration (based on description below)
3. Run jar file
$ java -jar remedy-tsi-bulkingestion-script-0.9.8.jar
4. Please read the output & provide further required choices.
```
```
NOTE:
1. You can also provide your choices as command line arguments.
Ex. $java -jar remedy-tsi-bulkingestion-script-0.9.8.jar <incident> <change> <exportincident> <exportchange> <silent> <retry> <loglevel>
2. You can enable the debug mode by having debug as command line parameter 
Ex. $java -jar remedy-tsi-bulkingestion-script-0.9.8.jar debug
3. The arguments can be used in any combination and any order.
```
 Each argument has a following meaning and effect.
 
   | Argument        | Description													|
   |-----------------|---------------------------------------------------------------|
   |incident		 | The script ingests the incidents.     			 		| 
   |Change			 | The script ingests the change tickets.			   	   	|
   |exportincident   | The script exports the ingested incidents into a CSV file.     |
   |exportchange     | The script exports the ingested change tickets into a CSV file.|
   |silent    		 | This argument will start the script in silent mode. It would start the ingestion based on the other arguments and does not prompt the user for further inputs.|
   |retry		     | The script dumps a csv file with the invalid events, retry argument makes the script to read from the csv and try again to ingest these events.|
   |loglevel ex debug| This sets the logging level									|

## How to build this project? 
```
Step 1: Obtain the Remedy Java API SDK jar.
Step 2: Add the jar to local m2 repository.
$ mvn install:install-file -DgroupId=com.bmc.arsys.api -DartifactId=api80_build002 -Dversion=8.0 -Dpackaging=jar -Dfile=<directorylocation>\api80_build002-8.0.jar -DgeneratePom=true
Step 3: Clone the integration library 
$ git clone https://github.com/boundary/remedy-tsi-integration-lib.
Step 4: Build it with Maven.
$ mvn clean compile install
Step 5: Clone this repository.
$ git clone https://github.com/boundary/remedy-tsi-bulkingestion-script.git
Step 6: Change the directory.
$ cd remedy-tsi-bulkingestion-script
Step 7: Run maven install.
$ mvn clean install
Step 8: You can find the build jar file as target/remedy-tsi-bulkingestion-script-0.9.8.jar

##### Note : You can also find a pre-built jar in dist folder
```
## Configuration
   The configuration file contains three major sections.

    1. "config":{}
    2. "eventDefinition":{}
    3. "fieldDefinitionMap":{}


### 1) Config

#### The config element contains all the required configuration to run this script.
```
"config": {
  "remedyHostName":"xxxx"  					---> ARServer Host name
  "remedyPort":"0"  						---> ARServer TCP port
  "remedyUserName":"xxxx",  					---> ARServer UserName
  "remedyPassword":"xxxx",					---> Password
  "tsiApiToken":"xxxx",       					---> TSI API Token
  "startDateTime":"2016-12-31 00:00:00 UTC", 	 		---> Start Date for Remedy conditionFields
  "endDateTime":"2017-12-31 00:00:00 UTC", 			---> End date for remedy conditionFields
}

```


### 2) Event Definition
```
Payload field and value						Details/Comment
"eventDefinition": {						---> Event Definition
  "properties": {
    "app_id": "CHANGE_ME" 			---> Change the App_Id as Required
  }
 }
```

### 3) Field Definition Map

There are several Field Definitions/ Placeholder definitions already available by default(see [IncidentDefaultTemplate](https://github.com/boundary/remedy-tsi-bulkingestion-script/blob/master/templates/incidentDefaultTemplate.json) & [ChangeDefaultTemplate](https://github.com/boundary/remedy-tsi-bulkingestion-script/blob/master/templates/changeDefaultTemplate.json) ).

## How to add a Field definition and map the property to eventDefinition?

Add a definition as a property of the "fieldDefinitionMap"
```
"fieldDefinitionMap":{
	....
	"@CUSTOMFIELD": {
			"fieldId":1000000162,
			"valueMap":{
				"1000": "Critical",
				"2000": "High",
				"3000": "Medium",
				"4000": "Low"
			}
		},
```


Use the definition key as a value of the property of eventDefinition.properties.
Example
```
{
"eventDefinition": {						  
		"properties": {
			"app_id": "Remedy Ingestion script",
			"CustomFieldName":"@CUSTOMFIELD"		// Add property like this
		}
 },
 "fieldDefinitionMap":{
 	...
	 "@CUSTOMFIELD": {
			"fieldId":1000000162,
			"valueMap":{
				"1000": "Critical",
				"2000": "High",
				"3000": "Medium",
				"4000": "Low"
			}
		}
	}
}
```
> 1. The placeholder definition contains the Remedy `fieldId` , which defines that this fieldId's value from Remedy entry will be used in place of this placeholder.

> 2. If valueMap is present in the definition (If it is a selection field) its value would be used from valueMap otherwise the Remedy value will be used.

## Frequently Asked Questions

Q1. Where should I input the configuration details ?

Answer: You should open the folder dist and input the parameters in the config and change the app_id with your App Id. The JSON templates in ./dist folder are the User Template which are the script configuration file.

Q2. Why there is no property mapping (apart from app_id) in the incidentTemplate/changeTemplate? What happens if the script is executed with only app_id in the properties?

Answer: There is a set of default fields(ref- ./templates/incidentDefaultTemplate.json & ./templates/changeDefaultTemplate) which are loaded and used along with app_id if the incidentTemplate/changeTemplate (User Templates) contains app_id as the only property.

Q3. How can I add/remove/change the properties mapped in the template?

Answer: If the user template has more property than app_id, then script loads & uses only the properties mapped in the User Template. It completely ignores/overwrites the default property mapping. So if you want to add/remove/update the property mapping, you can take the default templates(see in ./template/*) as sample and copy the JSON content to the user template after making required changes.

Q4. I want to contribute to the project. How do I obtain the Remedy Api Sdk jar ?

Answer: You can contact Remedy or Truesight Intelligence team to get this dependency.
