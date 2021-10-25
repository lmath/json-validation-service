## json-validation-service 

### json schema

Have you ever thought, json is really flexible and cool but wouldn't it be nice if sometimes there 
were a shared language & standard between those providing and those reading json?

Enter json schema! More info [here](http://json-schema.org/).

### this json-validation-service

Now have you ever thought I have a schema and a json document, and I want to know if if the json 
document conforms to a schema, and I just want to ask a web service? 

Enter this json-validation-service! 

![image](https://user-images.githubusercontent.com/3072877/138741108-ae6d688c-de34-458a-bef5-ef24000da322.png)

json-validation-service implements the API described [here](https://gist.github.com/goodits/20818f6ded767bca465a7c674187223e):

Posted here too for reference:
```
POST    /schema/SCHEMAID        - Upload a JSON Schema with unique `SCHEMAID`
GET     /schema/SCHEMAID        - Download a JSON Schema with unique `SCHEMAID`

POST    /validate/SCHEMAID      - Validate a JSON document against the JSON Schema identified by `SCHEMAID`
```

Also note that as recommended, we use [json-schema-validator](https://github.com/daveclayton/json-schema-validator)
to actually validate the submitted documents against schemas we have stored. 

### Running json-validation-service locally

This is a play 2.8.8 app running on scala 2.13.6.

Dependencies:
- scala 2, info on installing [here](https://www.scala-lang.org/download/scala2.html)
- java 11 or higher please (I did not have time to test this compiled for java 8, more details [here](https://docs.scala-lang.org/overviews/jdk-compatibility/overview.html))
- [sbt](https://www.scala-sbt.org/1.x/docs/Setup.html) 
- [docker](https://docs.docker.com/get-docker/)


#### starting elasticsearch
Navigate to 
`/json-validation-service/elasticsearch` in your terminal

run `docker-compose up`

Try this command to see if you've connected OK to the elasticsearch node:
```
curl --location --request GET 'localhost:9200/_cat/indices'
```

#### starting the json-validation-service
In directory `/json-validation-service` run:
```
sbt run
```

#### try it out
The API for json-validation-service requires you to provide your own unique ids. 

For the purposes of trying out the service, it entertains me to use the following to generate
ids:
https://www.correcthorsebatterystaple.net/

Then you can try out the test case described [here](https://gist.github.com/goodits/20818f6ded767bca465a7c674187223e):

submit a schema:
```
curl --location --request POST 'localhost:9000/schema/Inkjet-Delay-Century-Profits' \
--header 'Content-Type: application/json' \
--data-raw '{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "source": {
      "type": "string"
    },
    "destination": {
      "type": "string"
    },
    "timeout": {
      "type": "integer",
      "minimum": 0,
      "maximum": 32767
    },
    "chunks": {
      "type": "object",
      "properties": {
        "size": {
          "type": "integer"
        },
        "number": {
          "type": "integer"
        }
      },
      "required": ["size"]
    }
  },
  "required": ["source", "destination"]
}'
```

then use it to check if your document is valid

```
curl --location --request POST 'localhost:9000/validate/Inkjet-Delay-Century-Profits' \
--header 'Content-Type: application/json' \
--data-raw '{
  "source": "/home/alice/image.iso",
  "destination": "/mnt/storage",
  "timeout": null,
  "chunks": {
    "size": 1024,
    "number": null
  }
}'
```


