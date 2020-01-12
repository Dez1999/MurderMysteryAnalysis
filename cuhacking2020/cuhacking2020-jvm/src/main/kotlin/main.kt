import com.mongodb.BasicDBObject
import org.json.JSONObject
import org.json.JSONTokener
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException

import com.mongodb.ClientSessionOptions
import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import com.mongodb.client.*
import com.mongodb.client.model.Filters.*
import com.mongodb.connection.ClusterDescription
import org.bson.Document
import org.bson.conversions.Bson

import java.lang.Integer.parseInt


val NUMPEOPLE: Int = 12

//NOTE: updateLocation function will be updated for every case
// SetPersLocation, didExit, didEnter, and discrepancy are all reusable

val currentRoom = IntArray(NUMPEOPLE)
val layout = Layout()

val device = 0
val guestId = 1
val deviceId = 2
val time = 3
val event = 4

val uri = MongoClientURI("mongodb+srv://dbMongo1:<Welcome2DB>@cluster0-4xlpu.gcp.mongodb.net/test?retryWrites=true&w=majority")
var mongoClient = MongoClient()

enum class events
{
    UnlockedNKC, SuccUnlocKC, UserCon, UserDiscon, DoorClose, MotionDet, OffHook, OnHook, NewClient, Unkown
}

fun main(args: Array<String>)
{
    layout.populate()

    mongoClient = object : MongoClient(uri) {}

    val database = mongoClient.getDatabase("test")  //database
    if (database == null)
        println("Death")
    /*try {
        database.createCollection("Raw_Data");
    }catch(MongoCommandException e){*/
    val rawData = database!!.getCollection("Raw_Data")
    //}

    try {
        //Parsing the contents of the JSON file
        val token = JSONTokener(FileReader("/Users/stephenator/Downloads/Murder-on-the-2nd-Floor-Raw-Data.json"))
        val jsonObject = JSONObject(token)
        //(JSONObject) jsonParser.parse(new FileReader("hack_data.json"));
        //Read JSON file

        //JSONArray jsonArray = (JSONArray) jsonObject.get("dataset");
        //Iterate over the json file
        val iterator = jsonObject.keys()
        var l = 0
        //Document doc = new Document("Time", "deviceId", "device", "event", "guestId")

        while (iterator.hasNext()) {
            l += 1
            val Timestring = iterator.next()
            val eventInfo = jsonObject.getJSONObject(Timestring)
            val Time = parseInt(Timestring)
            val doc = Document("device-id", eventInfo.getString("device-id"))
            doc.append("device", eventInfo.getString("device"))
            doc.append("event", eventInfo.getString("event"))
            doc.append("guestId", eventInfo.getString("guest-id"))
            doc.append("Time", Time)

            rawData.insertOne(doc)
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}


fun getInfo(eventIndex: Int): Array<Any>
{
    val database = mongoClient.getDatabase("test")
    val rawData = database!!.getCollection("Raw_Data")
    val info = rawData.find(Document()).sort(BasicDBObject("Time", "1")).skip(eventIndex).first()
    return info.values.toTypedArray()
}

fun getCount(): Long
{
    val database = mongoClient.getDatabase("test")
    val rawData = database!!.getCollection("Raw_Data")
    return rawData.countDocuments()
}

fun addToSummary(groupId: String, time: Int, location: Int)
{
    val database = mongoClient.getDatabase("test")
    val summary = database!!.getCollection("summary")

    val doc = Document("group-id", groupId)
    doc.append("time", time)
    doc.append("location", location)

    summary.insertOne(doc)
}

fun remFromSummary(groupId: String, time: Int, location: Int)
{
    val database = mongoClient.getDatabase("test")
    val summary = database!!.getCollection("summary")

    val doc = Document("group-id", groupId)
    doc.append("time", time)
    doc.append("location", location)

    summary.deleteOne(and(eq("group-id", groupId), eq("time", time), eq("location", location)))
}

fun SetPersLocation(eventIndex: Int)
{
    var data = getInfo(eventIndex)
    println(data[0])
    println(data[1])
    println(data[2])
    println(data[3])
    println(data[4])
    discrepancy(eventIndex)
    //TODO: implement phones
    if(data[device] == "phone") {
        return
    }

    var event: events = convertEventToInt(data[event].toString())
    when (event) {
        events.UnlockedNKC -> didExit(eventIndex)
        events.SuccUnlocKC -> didEnter(eventIndex)
        events.UserCon -> updateLocation(convertPersonToInt(data[guestId].toString()), data[time].toString().toInt(), data[deviceId].toString())
        events.UserDiscon -> updateLocation(convertPersonToInt(data[guestId].toString()), data[time].toString().toInt(), data[deviceId].toString())
        else -> println("ya suck")
    }

}

//NOTE: We assume if they open the door but don't exit and someone is standing in the hallway the person in
// the hallway enters the room
fun didExit(eventIndex: Int)
{
    var data = getInfo(eventIndex)
    var index = eventIndex + 1

    for (person in 0 until NUMPEOPLE)
    {
        if(data[deviceId].toString().toInt() == currentRoom[person])
        {
            while(index < getCount())
            {
                var dataIndex = getInfo(index)
                if(dataIndex[guestId].toString() == convertPersonToString(person))
                {
                    updateLocation(person, data[time].toString().toInt(), data[deviceId].toString())
                    break
                }
                if(dataIndex[deviceId] == data[deviceId] &&
                        convertEventToInt(dataIndex[event].toString()) == events.UnlockedNKC)
                { // they don't leave the room
                    // check if anyone is standing outside
                    for(hallwayPerson in 0 until NUMPEOPLE)
                    {
                        if(currentRoom[hallwayPerson] == layout.getNextTo(data[deviceId].toString().toInt()))
                        {
                            updateLocation(hallwayPerson, data[time].toString().toInt(), data[deviceId].toString())
                        }
                    }
                    break
                }
                index++
            }
        }
    }
}


fun didEnter(eventIndex: Int)
{
    var data = getInfo(eventIndex)
    var index = eventIndex
    var exited: Boolean = false

    while(index < getCount())
    {
        var dataIndex = getInfo(index)
        if(convertEventToInt(dataIndex[event].toString()) == events.UnlockedNKC &&
                dataIndex[deviceId] == data[deviceId])
        {
            //NOTE: This does not account for someone else leaving the room instead of this person
            exited = true
        }
        if(dataIndex[guestId] == data[guestId])
        {
            if(exited)
            {
                updateLocation(convertPersonToInt(data[guestId].toString()), data[time].toString().toInt(), data[deviceId].toString())
            }
        }
        index++
    }
}

//NOTE: any fixes for discrepancies assume that the latest door events were when the person left
// this means that the timestamp for people leaving a room has some proneness to error
// This also assumes that there will not be a case where someone goes from point a to point b without more than one
// door being held open for them
// Discrepancies can only happen with doors because the error comes with the possibility of opening doors for others
fun discrepancy(eventIndex: Int)
{
    var data = getInfo(eventIndex)
    var index = eventIndex - 1

    if(data[guestId].toString() != "n/a")
    {
        var person = convertPersonToInt(data[guestId].toString())
        var expectedRoom: Int = currentRoom[person]
        var newRoom: Int = convertDeviceToLocation(data[deviceId].toString())
        if(!layout.nextTo(expectedRoom, newRoom))
        { // There is a discrepancy, need to backtrack to find error
            if(layout.getNextTo(expectedRoom) > 0)
            {
                var dataIndex = getInfo(index)
                //check if the event is them entering the expected room
                // (we don't want them leaving a room before entering it, no time travellers allowed)
                while(dataIndex[guestId].toString() != convertPersonToString(person) ||
                        expectedRoom != convertDeviceToLocation(dataIndex[deviceId].toString()) ||
                        convertEventToInt(dataIndex[event].toString()) != events.SuccUnlocKC)
                {
                    dataIndex = getInfo(index)
                    //if someone exited the room that we thought they were in
                    if(expectedRoom == convertDeviceToLocation(dataIndex[deviceId].toString()) &&
                            convertEventToInt(dataIndex[event].toString()) == events.UnlockedNKC)
                    {
                        updateLocation(person, dataIndex[time].toString().toInt(), dataIndex[deviceId].toString())
                        return
                    }
                    index--
                }
                // we did not find a time they could have left (no doors were opened)
                // this means our original assumption of them entering the room was wrong and needs to be removed
                removeLocation(person, index)
            }
        }
    }
}

//**********************************************************************************************************************
//                            All of the following functions change content for each case
//**********************************************************************************************************************
class Layout {
    private var adjMatrix: Array<BooleanArray> = Array(33) { BooleanArray(33) }

    fun populate() {
        for (i in 0 .. 32)
        {

            for (j in 0 .. 32) {
                adjMatrix[i][j] = false
            }
            adjMatrix[i][i] = true
        }
//rooms to hallway
        adjMatrix[1][11] = true
        adjMatrix[2][12] = true
        adjMatrix[3][32] = true
        adjMatrix[4][32] = true
        adjMatrix[5][32] = true
        adjMatrix[5][6] = true
        adjMatrix[6][5] = true
        adjMatrix[7][32] = true
        adjMatrix[8][32] = true
        adjMatrix[9][32] = true
        adjMatrix[10][11] = true
        adjMatrix[11][29] = true//hallway
        adjMatrix[11][12] = true//hallway
        adjMatrix[11][31] = true//hallway
        adjMatrix[11][32] = true//hallway
        adjMatrix[12][32] = true//hallway

        adjMatrix[13][24] = true
        adjMatrix[14][24] = true
        adjMatrix[15][24] = true
        adjMatrix[16][25] = true
        adjMatrix[17][26] = true
        adjMatrix[18][26] = true
        adjMatrix[19][26] = true
        adjMatrix[20][26] = true
        adjMatrix[21][25] = true
        adjMatrix[22][25] = true
        adjMatrix[23][24] = true
        adjMatrix[24][25] = true //hallway
        adjMatrix[25][26] = true
        adjMatrix[27][25] = true
        adjMatrix[7][26] = true
//reverse
        adjMatrix[11][1] = true
        adjMatrix[12][2] = true
        adjMatrix[32][3] = true
        adjMatrix[32][4] = true
        adjMatrix[32][5] = true
        adjMatrix[32][7] = true
        adjMatrix[32][8] = true
        adjMatrix[32][9] = true
        adjMatrix[11][10] = true
        adjMatrix[29][11] = true//hallway
        adjMatrix[12][11] = true//hallway
        adjMatrix[31][11] = true//hallway
        adjMatrix[32][11] = true//hallway
        adjMatrix[32][12] = true//hallway

        adjMatrix[24][13] = true
        adjMatrix[24][14] = true
        adjMatrix[24][15] = true
        adjMatrix[25][16] = true
        adjMatrix[26][17] = true
        adjMatrix[26][18] = true
        adjMatrix[26][19] = true
        adjMatrix[26][20] = true
        adjMatrix[25][21] = true
        adjMatrix[25][22] = true
        adjMatrix[25][29] = true
        adjMatrix[29][25] = true
        adjMatrix[24][23] = true
        adjMatrix[25][24] = true
        adjMatrix[26][25] = true
        adjMatrix[25][27] = true
        adjMatrix[26][7] = true
    }

    fun nextTo(roomOne: Int, roomTwo: Int): Boolean
    {
        return adjMatrix[roomOne][roomTwo]
    }

    fun getNextTo(roomNum: Int): Int {
        var sum: Int = 0
        if(roomNum == 5){
            return 32
        }
        for (i in 1..32) {
            sum += if(adjMatrix[roomNum][i]) 1 else 0

        }
        if (sum == 2){
            for( i in 0..32){
                if (i != roomNum && adjMatrix[roomNum][i])
                    return i
            }
        }
        return -1
    }
}

fun convertDeviceToLocation(deviceId: String): Int
{
    return when(deviceId)
    {
        "ap1-1" -> 1
        "110" -> 1
        "130" -> 2
        "152" -> 3
        "154" -> 4
        "156" -> 5
        "156b" -> 6
        "stairwell" -> 7
        "155" -> 8
        "151" -> 9
        "101" -> 10
        "ap1-4" -> 11
        "ap1-3" -> 12
        "210" -> 13
        "220" -> 14
        "232" -> 15
        "236" -> 16
        "244" -> 17
        "248" -> 18
        "247" -> 19
        "241" -> 20
        "235" -> 21
        "233" -> 22
        "231" -> 23
        "ap2-1" -> 24
        "ap2-3" -> 25
        "ap2-2" -> 26
        "ice machine" -> 27
        "reception" -> 28
        "elevator" -> 29
        "Leave" -> 31
        "ap1-2" -> 32
        else -> 0
    }
}

fun convertPersonToInt(name: String): Int
{
    return when(name)
    {
        "Jason" -> 0
        "Dave" -> 1
        "Veronica" -> 2
        "Thomas" -> 3
        "Alok" -> 4
        "Eugene" -> 5
        "Marc-Andre" -> 6
        "Rob" -> 7
        "Kristina" -> 8
        "Salina" -> 9
        "Harrison" -> 10
        "James" -> 11
        else -> -1
    }
}

fun convertPersonToString(person: Int): String
{
    return when(person)
    {
        0 -> "Jason"
        1 -> "Dave"
        2 -> "Veronica"
        3 -> "Thomas"
        4 -> "Alok"
        5 -> "Eugene"
        6 -> "Marc-Andre"
        7 -> "Rob"
        8 -> "Kristina"
        9 -> "Salina"
        10 -> "Harrison"
        11 -> "James"
        else -> "n/a"
    }
}

fun convertEventToInt(event: String): events
{
    return when(event)
    {
        "unlocked no keycard" -> events.UnlockedNKC
        "successful keycard unlock" -> events.SuccUnlocKC
        "user connected" -> events.UserCon
        "user disconnected" -> events.UserDiscon
        "door closed" -> events.DoorClose
        "motion detected" -> events.MotionDet
        "off hook" -> events.OffHook
        "on hook" -> events.OnHook
        "new client" -> events.NewClient
        else -> events.Unkown
    }
}

fun updateLocation(person: Int, time: Int, deviceId: String)
{
    var newRoom: Int = convertDeviceToLocation(deviceId)
    var personString = convertPersonToString(person)

    currentRoom[person] = newRoom

    addToSummary(personString, time, newRoom)
}

fun removeLocation(person: Int, eventIndex: Int)
{
    var data = getInfo(eventIndex)
    var newRoom: Int = convertDeviceToLocation(data[deviceId].toString())
    var time = data[time].toString().toInt()
    var guestId = data[guestId].toString()

    remFromSummary(guestId, time, newRoom)
}
