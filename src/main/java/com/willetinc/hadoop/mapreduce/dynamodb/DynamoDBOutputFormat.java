/**
 * Copyright 2012 Willet Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.willetinc.hadoop.mapreduce.dynamodb;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.willetinc.hadoop.mapreduce.dynamodb.io.DynamoDBKeyWritable;

/**
 * 
 */
public class DynamoDBOutputFormat<K extends DynamoDBKeyWritable, V> extends
		OutputFormat<K, V> {

	@Override
	public void checkOutputSpecs(JobContext context)
			throws IOException,
			InterruptedException {

	}

	@Override
	public OutputCommitter getOutputCommitter(TaskAttemptContext context)
			throws IOException,
			InterruptedException {
		return new FileOutputCommitter(
				FileOutputFormat.getOutputPath(context),
				context);
	}

	public class DynamoDBRecordWriter extends RecordWriter<K, V> {

		private AmazonDynamoDBClient client;

		private String tableName;

		public DynamoDBRecordWriter() {
		};

		public DynamoDBRecordWriter(AmazonDynamoDBClient client,
				String tableName) {
			this.client = client;
			this.tableName = tableName;
		}

		@Override
		public void close(TaskAttemptContext context)
				throws IOException,
				InterruptedException {
			if (null != client) {
				client.shutdown();
			}
		}
		
		public AmazonDynamoDBClient getClient() {
			return client;
		}
		
		public String getTableName() {
			return tableName;
		}

		@Override
		public void write(K key, V value)
				throws IOException,
				InterruptedException {
			Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
			key.write(item);

			PutItemRequest putItemRequest = new PutItemRequest().withTableName(
					tableName).withItem(item);

			client.putItem(putItemRequest);
		}

	}
	
	@Override
	public RecordWriter<K, V> getRecordWriter(TaskAttemptContext context)
			throws IOException,
			InterruptedException {
		DynamoDBConfiguration dbConf = new DynamoDBConfiguration(
				context.getConfiguration());
		return new DynamoDBRecordWriter(
				dbConf.getAmazonDynamoDBClient(),
				dbConf.getOutputTableName());
	}

	/**
	 * Method used for unit testing DynamoDBRecordWriter. Should not be called
	 * otherwise.
	 * 
	 * @param client
	 *            Configured AmazonDynamoDBClient.
	 * @param tableName
	 *            DynamoDb table name
	 * @return Configured DynamoDBRecordWriter instance
	 */
	RecordWriter<K, V> getRecordWriter(
			AmazonDynamoDBClient client,
			String tableName) {
		return new DynamoDBRecordWriter(client, tableName);
	}

}
