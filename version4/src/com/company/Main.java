package com.company;

import com.mongodb.ClientSessionOptions;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCommandException;
import com.mongodb.client.*;
//import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterDescription;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static java.lang.Integer.parseInt;

;
;



public class Main {

    public static void main(String[] args) {

        MongoClientURI uri = new MongoClientURI(
                "mongodb+srv://dbMongo1:<password>@cluster0-4xlpu.gcp.mongodb.net/test?retryWrites=true&w=majority");


        MongoClient mongoClient = new MongoClient()

        {
            @Override
            public MongoDatabase getDatabase(String s) {
                return null;
            }

            @Override
            public ClientSession startSession() {
                return null;
            }

            @Override
            public ClientSession startSession(ClientSessionOptions clientSessionOptions) {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public MongoIterable<String> listDatabaseNames() {
                return null;
            }

            @Override
            public MongoIterable<String> listDatabaseNames(ClientSession clientSession) {
                return null;
            }

            @Override
            public ListDatabasesIterable<Document> listDatabases() {
                return null;
            }

            @Override
            public ListDatabasesIterable<Document> listDatabases(ClientSession clientSession) {
                return null;
            }

            @Override
            public <TResult> ListDatabasesIterable<TResult> listDatabases(Class<TResult> aClass) {
                return null;
            }

            @Override
            public <TResult> ListDatabasesIterable<TResult> listDatabases(ClientSession clientSession, Class<TResult> aClass) {
                return null;
            }

            @Override
            public ChangeStreamIterable<Document> watch() {
                return null;
            }

            @Override
            public <TResult> ChangeStreamIterable<TResult> watch(Class<TResult> aClass) {
                return null;
            }

            @Override
            public ChangeStreamIterable<Document> watch(List<? extends Bson> list) {
                return null;
            }

            @Override
            public <TResult> ChangeStreamIterable<TResult> watch(List<? extends Bson> list, Class<TResult> aClass) {
                return null;
            }

            @Override
            public ChangeStreamIterable<Document> watch(ClientSession clientSession) {
                return null;
            }

            @Override
            public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, Class<TResult> aClass) {
                return null;
            }

            @Override
            public ChangeStreamIterable<Document> watch(ClientSession clientSession, List<? extends Bson> list) {
                return null;
            }

            @Override
            public <TResult> ChangeStreamIterable<TResult> watch(ClientSession clientSession, List<? extends Bson> list, Class<TResult> aClass) {
                return null;
            }

            //@Override
            public ClusterDescription getClusterDescription() {
                return null;
            }
        };

       MongoDatabase database = mongoClient.getDatabase("test");  //database
        if(database == null)
            System.out.println("Death");
        /*try {
            database.createCollection("Raw_Data");
        }catch(MongoCommandException e){*/
            MongoCollection<Document> rawData = database.getCollection("Raw_Data");
        //}

        try {
            //Parsing the contents of the JSON file
            JSONTokener token = new JSONTokener(new FileReader("C:/Users/Blake/Desktop/Cuhacking 2020/martello-hack-data-v1.json"));
            JSONObject jsonObject = new JSONObject(token);
            //(JSONObject) jsonParser.parse(new FileReader("hack_data.json"));
            //Read JSON file

            //JSONArray jsonArray = (JSONArray) jsonObject.get("dataset");
            //Iterate over the json file
            Iterator<String> iterator = jsonObject.keys();
            int l =0;
            //Document doc = new Document("Time", "deviceId", "device", "event", "guestId")

            while (iterator.hasNext()) {
                l += 1;
                String Timestring = iterator.next();
                JSONObject eventInfo = jsonObject.getJSONObject(Timestring);
                int Time = parseInt(Timestring);
                String deviceId = eventInfo.getString("device-id");
                String device = eventInfo.getString("device");
                String event = eventInfo.getString("event");
                String guestId = eventInfo.getString("guest-id");
                Document doc = new Document("device-id", eventInfo.getString("device-id"));
                    doc.append("device", eventInfo.getString("device"));
                    doc.append("event", eventInfo.getString("event"));
                    doc.append("guestId", eventInfo.getString("guest-id"));
                    doc.append("Time", Time);

                    rawData.insertOne(doc);
                //database.insert("Time");

                //Method to save into Mongodb
            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }





}

