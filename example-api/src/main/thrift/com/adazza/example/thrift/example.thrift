namespace java com.adazza.example.thrift
#@namespace scala com.adazza.ingest.thrift

service ExampleService {
    string ping(1: string pong)
}
