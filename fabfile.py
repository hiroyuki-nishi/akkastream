import subprocess
import configparser
import boto3
import json
import random, string

from fabric.decorators import task

ini = configparser.SafeConfigParser()
ini.read("./config.ini")
que_url = ini.get("sqs", "que_url")
dynamodb_url = ini.get("dynamodb", "dynamodb_url")
kinesis_url = ini.get("kinesis", "kinesis_url")
region = ini.get("aws", "region")


def _client(service_name, endpoint_url, region=ini.get("aws", "region")):
    return boto3.client(service_name, endpoint_url=endpoint_url, region_name=region)


def _sqs_client():
    return _client('sqs', ini.get("sqs", "que_endpoint"))

def _dynamodb_client():
    return _client('dynamodb', ini.get("dynamodb", "dynamodb_url"))

def _random_name(n=10):
    randlst = [random.choice(string.ascii_letters + string.digits) for i in range(n)]
    return ''.join(randlst)

@task
def create_kinesis():
    subprocess.call("""
    aws kinesis create-stream --endpoint-url {0} --region {1} --cli-input-json file://cloudformation/akka/kinesis/kinesis.json
    """.format(kinesis_url, region), shell=True)

@task
def create_dynamodb():
    subprocess.call("""
    aws --endpoint-url={0} dynamodb create-table --cli-input-json file://cloudformation/akka/dynamodb/company.json
    """.format(dynamodb_url).split(), shell=True)

@task
def scan_table(table_name):
    subprocess.call("""
    aws --endpoint-url={0} --region ap-northeast-1 dynamodb scan --table-name {1} 
    """.format(dynamodb_url, table_name).split())

@task
def put_records(stream_name):
    subprocess.call("""
        aws --endpoint-url={1} kinesis put-record --stream-name {0} --partition-key 123 --data testdata
    """.format(stream_name, kinesis_url), shell=True)

@task
def send_queue(size=3, url=que_url):
    client = _sqs_client()
    for i in range(int(size)):
        client.send_message(
            QueueUrl=url,
            DelaySeconds=0,
            MessageBody=(
                json.dumps({"id": "id_" + _random_name(), "name": "name_" + _random_name()})
            )
        )

@task
def get_records():
    subprocess.call("""
        aws --endpoint-url={0} kinesis get-shard-iterator --shard-id shardId-000000000000 --shard-iterator-type TRIM_HORIZON --stream-name lspan-spa-kinesis --query 'ShardIterator' | xargs -L 1 -IXXX aws --endpoint-url={0} kinesis get-records --shard-iterator XXX
    """.format(kinesis_url), shell=True)

@task
def receive_queue(size=1, url=que_url):
    for i in range(int(size)):
        response = _sqs_client().receive_message(
            QueueUrl=url,
            AttributeNames=[
                'SentTimestamp'
            ],
            MaxNumberOfMessages=1,
            VisibilityTimeout=0,
            WaitTimeSeconds=0
        )
        print(response)


@task
def create_queue(queue_name='lspan-sqs'):
    print(_sqs_client().create_queue(
        QueueName=queue_name
    ))


@task
def delete_queue(url=que_url):
    print(_sqs_client().delete_queue(
        QueueUrl=url
    ))

@task
def list_kinesis():
    subprocess.call("aws --endpoint-url={0} kinesis list-streams".format(kinesis_url), shell=True)

@task
def list_queues():
    print(_sqs_client().list_queues())

@task
def delete_stream(stream_name):
    subprocess.call("aws --endpoint-url={0} kinesis delete-stream --stream-name {1}".format(kinesis_url, stream_name), shell=True)

