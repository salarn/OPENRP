# OPENRP
Implement OPENRP API

Connecting devices:
We use Salut Library for connecting device with wifi-direct, This is Salut git repository: https://github.com/markrjr/Salut There have good tutorial and also feel free to ask us if there is any problem.
We define Host: a device that all clients can searching and find him and connect to him, But just client can search for Hosts and Hosts can't search and find each other. Hosts are like servers.
Also define Client: a device that can search for Hosts and find them and connect to them.
In our API at first all device initiate with Host type, And when receive request from application turn off there server network and became Client, and now can search for Host device and connect to them and send his Shake Request (first request that define in paper with name REQ).

Database:
We use android SQLite library to save our tables locally.
Now we have just one table it name is communicationData, we have 4 column on that. Column names are counter, peer_id, time, value. counter is our primary key, and peer_id is peer android id (it's unique), time is the time we add this row to our database, and value is float between 0 to 1 and it's our score that give to peer for doing our task or reject our task. We calculate reputation of each device with a formula from this values.

Sending requests:
Salut use LoganSquare library for sending requests, We implement class that name is Request and we create our requests with type Request. In Request we have 3 String objects and that names are requestTitle, requestDetail, requestPeerId. Device can understand type of requests with requestTitle for example: REQ,RESP,TASK,ANS. requestPeerId is our android id. And requestDetail is content of our request, We Create requestDetail in JSON type. Also I suggest use GSON library that is a library from google that can convert all java's object to JSON and vise-versa. this is link of GSON library: https://github.com/google/gson
