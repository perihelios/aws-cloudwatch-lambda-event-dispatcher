# AWS CloudWatch Event Dispatcher for AWS Lambda
[![Maven Central](https://img.shields.io/maven-central/v/com.perihelios.aws/cloudwatch-lambda-event-dispatcher.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.perihelios.aws%22%20AND%20a:%22cloudwatch-lambda-event-dispatcher%22)

This library allows you to easily accept and processes CloudWatch events in a Java AWS Lambda.

## Usage Example

Consider the following CloudWatch event (from the
[AWS Docs](https://docs.aws.amazon.com/AmazonCloudWatch/latest/events/EventTypes.html#ec2_event_type)).

```
{
   "id":"7bf73129-1428-4cd3-a780-95db273d1602",
   "detail-type":"EC2 Instance State-change Notification",
   "source":"aws.ec2",
   "account":"123456789012",
   "time":"2015-11-11T21:29:54Z",
   "region":"us-east-1",
   "resources":[
      "arn:aws:ec2:us-east-1:123456789012:instance/i-abcd1111"
   ],
   "detail":{
      "instance-id":"i-abcd1111",
      "state":"pending"
   }
}
```

Create a class to map this event. Use the `detail-type` property value in the `@DetailType` annotation and add fields
from the `detail` property. Create an `enum` for the possible values in `state`, based on the AWS docs linked
previously.

```
import com.google.gson.annotations.SerializedName;
import CloudWatchEvent;
import DetailType;

@DetailType("EC2 Instance State-change Notification")
public class Ec2InstanceStateChangeEvent extends CloudWatchEvent {
    @SerializedName("instance-id")
    private String instanceId;
    
    private Ec2InstanceState state;
    
    public String instanceId() {
        return instanceId;
    }
    
    public Ec2InstanceState state() {
        return state;
    }
}
```

```
import com.google.gson.annotations.SerializedName;

public enum Ec2InstanceState {
    @SerializedName("running")
    RUNNING,
    
    @SerializedName("pending")
    PENDING,
    
    @SerializedName("shutting-down")
    SHUTTING_DOWN,
    
    @SerializedName("stopped")
    STOPPED,
    
    @SerializedName("stopping")
    STOPPING,
    
    @SerializedName("terminated")
    TERMINATED,
    
    ;
}
```

Create a class with a method to be invoked by AWS Lambda. Create an instance of the dispatcher from this library,
register a handler for the new custom event type, and dispatch events to the handler.

```
import com.amazonaws.services.lambda.runtime.Context;
import CloudWatchEventDispatcher;

import java.io.InputStream;

public class Lambda {
    public void handle(InputStream message, Context context) {
        new CloudWatchEventDispatcher(message, context)
            .withEventHandler(Ec2InstanceStateChangeEvent.class, (event, ctx) -> {
                ctx.getLogger().log("Instance " + event.instanceId() +
                    " has changed to state " + event.state());
            })
            .dispatch();
    }
}
```

You can build a JAR or Zip file of these classes and upload it to AWS Lambda. If you configure it as a destination for
`EC2 Instance State-change Notification` CloudWatch events, it will log EC2 instance state changes to CloudWatch Logs.
Try it out for yourself—and modify the handler to do something more interesting than just logging!

## Dependencies
[![Maven Central](https://img.shields.io/maven-central/v/com.perihelios.aws/cloudwatch-lambda-event-dispatcher.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.perihelios.aws%22%20AND%20a:%22cloudwatch-lambda-event-dispatcher%22)

This library is published to the
[Central Repository](https://search.maven.org/search?q=g:com.perihelios.aws%20AND%20a:cloudwatch-lambda-event-dispatcher).
It depends on Google GSON and the AWS Lambda Core libraries. You can include the library in your project with any of a
number of build systems; the two most common are provided below.

### Gradle
```
implementation("com.perihelios.aws:cloudwatch-lambda-event-dispatcher:1.0.0")
```

### Maven
```
<dependency>
  <groupId>com.perihelios.aws</groupId>
  <artifactId>cloudwatch-lambda-event-dispatcher</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Documentation
[Javadocs](https://perihelios.github.io/aws-cloudwatch-lambda-event-dispatcher/javadoc/) are available for this library.

## License

Copyright © 2019 Perihelios LLC.

Licensed under the [Apache 2.0 License](LICENSE.txt).
