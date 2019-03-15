# fea-service-backend

Web service based on the Spring Boot framework.
It is one half of a f-e-a.de expansion/tool, which delivers a list of related, already answered, questions to a given question based on a set of sample data. It also offers a text evaluator, which can be used to check if an answer written by a lawyer is possibly too complex for the average client, as well as statistical data for the sample data.
The other half of the tool is the frontend, which can be found here: [LINK]

This project is written in Java (JDK 8) and uses the following technologies:
- Solr
- IBM Watson Natural Language Understanding
- IBM Watson Natural Language Classifier
- OpenNLP
- JoBimText

# Installation:
 - Check out the project
 - Set up a Solr instance and create a core (Replace the 'solr_core_uri' in the ApplicationController with your own)
 - Create an account for IBM's Watson Services, create an instance of the Natural Language Understanding Service and place your endpoint in the 'ibm_watson_endpoint' variable and your api key in the 'api_key' variable, as well as the version in the 'api_version' variable in the ApplicationController
 - Fill the Solr core with data using the SolrUploadTool. You'll need to specify the path to your source file there. A JSON-file with the following format will do:
```
{
  "data":
  [
    {
      "topic_id": 000000,
      "T_Date": "2019-01-01 23:59:59",
      "T_Subject": "String",
      "T_PRice": 00,
      "T_Message": "String",
      "T_Summary": "String",
      "R_Posted": "2019-01-01 23:59:59",
      "R_Message": "String",
      "tags": "tag1 tag2 tag3",
      "empfehlungen": 0
    },
    {
      ...
    },
    ...
    {
      ...
    }
  ]
}
```

# How to run:
- Run the ApplicationController
- Default port is 8080 (for deployment port can be edited in /resources/application.properties)
- Run the frontend (with adjusted port for backend access) and use it
