#!/bin/sh

export AWS_DEFAULT_REGION='ap-northeast-1'
SQS_QUEUE_NAME='lspan-sqs'
SQS_ENDPOINT='http://localhost:4576'
DYNAMODB_ENDPOINT='http://localhost:4569'
REGION='ap-northeast-1'

aws --endpoint-url="${SQS_ENDPOINT}" --region "${REGION}" sqs create-queue --queue-name ${SQS_QUEUE_NAME}
aws --endpoint-url="${DYNAMODB_ENDPOINT}" dynamodb create-table --cli-input-json file://cloudformation/akka/dynamodb/company.json

# 確認
aws --endpoint-url="${SQS_ENDPOINT}" --region "${REGION}" sqs list-queues
aws --endpoint-url="${SQS_ENDPOINT}" --region "${REGION}" sqs get-queue-attributes --queue-url "${SQS_ENDPOINT}/queue/${SQS_QUEUE_NAME}" --attribute-names  \
          policy \
          visibilitytimeout \
          maximummessagesize \
          messageretentionperiod \
          approximatenumberofmessages \
          approximatenumberofmessagesnotvisible \
          createdtimestamp \
          lastmodifiedtimestamp \
          queuearn \
          approximatenumberofmessagesdelayed \
          delayseconds \
          receivemessagewaittimeseconds \
          redrivepolicy
aws dynamodb list-tables --endpoint-url="${DYNAMODB_ENDPOINT}"

