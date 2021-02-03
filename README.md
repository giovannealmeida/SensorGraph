# SensorGraph 
[![Release](https://img.shields.io/badge/release-v0.0.1--alpha-blue)](https://jitpack.io/#giovannealmeida/SensorGraph)

:chart_with_upwards_trend: A graph for Android's sensors data visualization.

## Features

- Lightweight (only 1 class)
- Customizable
- Scrolls horizontally 

## Adding dependecy

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.giovannealmeida:SensorGraph:v0.0.1-alpha'
}
```

## Usage

### XML layout
```xml
<br.com.giovanne.sensorgraph.SensorGraph
    android:id="@+id/graph"
    android:layout_width="wrap_content"
    android:layout_height="200dp"
    android:layout_marginBottom="8dp"
    app:borderColor="#FF0000"
    app:borderWidth="1dp"
    app:valuesLineWidth="2dp"
    app:valuesTextSize="18sp"
    app:valuesTextColor="#000000"
    app:axisLineWidth="1dp"
    app:backgroundColor="#FFFF00" />
```

### Activity
```java
private SensorGraph graph

...

graph = findViewById(R.id.rGraphGy);
graph.setMaxAmountValues(GRAPH_MAX_VISIBLE_VALUES);

...
public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
        //Add the coordinates x, y and z
        graph.addPoints(event.values[0], event.values[1], event.values[2]);
    }
}

```

![preview1](https://github.com/giovannealmeida/SensorGraph/blob/master/media/preview-1.png)

## Known issues

- When a coordinate reach the maximum or mininum limit line, the value is adjusted but the values already drawn are not.
- Might break if the points are added too fast.
