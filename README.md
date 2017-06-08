# remedy-tsi-bulkingestion-script
Script for ingestion of historical Remedy Incidents and Change tickets into Truesight Intelligence as events.

## Description.

This Script enables a Remedy & TSI User to have intelligence on the historical Remedy incidents and change tickets. Based on the configurations present in the template(see in dist), this script reads the Remedy Incident & Change Tickets and after converting into a TSI event based on definitions in  `eventDefinition` it ingests the Events to TSI.

## Prerequisite 
```
Java Sdk 1.8
maven ( *Only required  if you want to build the code)
```
## How to build ? 
```
1. Clone this repository.
$ git clone https://github.com/boundary/remedy-tsi-bulkingestion-script.git
2. Change the directory.
$ cd remedy-tsi-bulkingestion-script
3. Run maven install
$ mvn clean install
4. You can find the build jar file as remedy-tsi-bulkingestion-script-0.0.1-SNAPSHOT-full.jar
```
##### Note : You can find a pre-built jar in dist folder

## How to run ?
```
1. Copy jar file to the location as dist and change directory as dist.
$cd dist
2. Change the incidentTemplate.json/changeTemplate.json configuration (based on description below)
3. Run jar file
$java -jar remedy-meter-script-0.0.1-SNAPSHOT-full.jar <incident> <change>
```
## Configuration
   The configuration file contains three major sections.


    1. "config":{}
    2. "eventDefinition":{}
    3. "@placeholder":{}


### 1) Config

#### The config element contains all the required configurations to run this script.
```
"config": {
  "remedyHostName":"xxxx"       	      ---> ARServer Host name
  "remedyPort":"",                        ---> ARServer port (Not required)
  "remedyUserName":"xxxx",                ---> ARServer UserName
  "remedyPassword":"xxxx",                ---> Password
  "tsiEventEndpoint": "https://api.truesight-staging.bmc.com/v1/events",   ---> TSI events ingestion API endpoint
  "tsiApiToken":"xxxx",                   ---> TSI API Token
  "chunkSize":100,                        --->  No of tickets read and ingested in one chunk
  "conditionFields":[1000000564,3], ---> List of fields to create condition (see below for details)
  "startDateTime":"2017-01-01 00:00 AM GMT+1:00", ---> Start Date for Remedy conditionFields
  "endDateTime":"2017-05-29 00:00 AM GMT+1:00",  ---> End date for remedy conditionFields
  "retryConfig":3,                        ---> Retry configuration, in case of failure
 "waitMsBeforeRetry":5000         --->Time in ms to wait before next retry
}

```

> ** You can enter multiple Remedy field Ids as conditions. The reader will read based on all these fields falling in the startDate & EndDate configured.*You can enter multiple Remedy field Ids as conditions. The reader will read based on all these fields falling in the startDate & EndDate configured.

for ex if [1000000564,3] is given & 1000000564 & 3  are fieldIds for ClosedDate & SubmittedDate correspondingly. Then Reader will read all the tickets that have closed date or submitted date falling under startDate & endDate 

### 2) Event Definition
```
Payload field and value				Details/Comment
"eventDefinition": {			---> TSi event definition json
	"title": "@TITLE",			---> Value with @ prefix is a placeholder
	"fingerprintFields": ["IncidentNumber"]    ---> If There is no @ in the value it is treated as it is
	.....
	.....
}
```

### 3) Placeholder definitions
```
"@SEVERITY": {
		"fieldId":1000000162,
		"valueMap":{
			"1000": "Critical",
			"2000": "High",
			"3000": "Medium",
			"4000": "Low"
		}
	},
```
> 1. The placeholder definition contains the remedy `fieldId` , Which defines that this fieldId's value from Remedy Entry will be used in place of this placeholder.

> 2. In case of valueMap is present in the definition its value would be used otherwise the Remedy value will be used.


