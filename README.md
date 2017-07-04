# remedy-tsi-bulkingestion-script
Script for ingestion of historical Remedy Incidents and Change tickets into Truesight Intelligence as events.
## Description.

This Script ingests historical Remedy incidents and change tickets into TSI. Based on the configuration present in the template(see in dist), this script reads the Remedy Incident & Change Tickets and after converting into a TSI event (based on definitions in  `eventDefinition`), it ingests the events to TSI.


## Prerequisite 
```
Java Sdk 1.8
maven (*Only required if you want to build the code)
```
## How to build ? 
```
1. Clone this repository.
$ git clone https://github.com/boundary/remedy-tsi-bulkingestion-script.git
2. Change the directory.
$ cd remedy-tsi-bulkingestion-script
3. Run maven install
$ mvn clean install
4. You can find the build jar file as remedy-tsi-bulkingestion-script-0.9.0.jar
```
##### Note : You can find a pre-built jar in dist folder

## How to run ?
```
1. Copy jar file to the location as dist and change directory as dist.
$cd dist
2. Change the incidentTemplate.json/changeTemplate.json configuration (based on description below)
3. Run jar file
$java -jar remedy-meter-script-0.9.0.jar <incident> <change>
```
## Configuration
   The configuration file contains three major sections.


    1. "config":{}
    2. "eventDefinition":{}
    3. "@placeholder":{}


### 1) Config

#### The config element contains all the required configuration to run this script.
```
"config": {
  "remedyHostName":"xxxx"  					---> ARServer Host name
  "remedyPort":0,                                               ---> ArServer TCP Port
  "remedyUserName":"xxxx",  					---> ARServer UserName
  "remedyPassword":"xxxx",					---> Password
  "tsiEventEndpoint": "https://api.truesight-staging.bmc.com/v1/events", ---> TSI events ingestion API endpoint
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
    "app_id": "Remedy Ingestion script" 			---> Change the App_Id as Required
  }
 }
```

### 3) Placeholder Definition 
There are several Field Definitions/ Placeholder definitions already available by default. You can add a custom Field Definition & use it in the properties section of eventDefinition as per requirement.
```
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

```
Example
"eventDefinition": {						  
		"properties": {
			"app_id": "Remedy Ingestion script",
			"CustomFieldName":"@CUSTOMFIELD"				---> Add Custom additional fields like this
		}
 }
```
> 1. The placeholder definition contains the Remedy `fieldId` , which defines that this fieldId's value from Remedy entry will be used in place of this placeholder.

> 2. If valueMap is present in the definition its value would be used from valueMap otherwise the Remedy value will be used.


