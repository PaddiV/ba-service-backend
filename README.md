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

# How to run:
- Check out and run the ApplicationController
- Default port is 8080 (for deployment port can be edited in /resources/application.properties)
- Run the frontend (with adjusted port for backend access) and use it
