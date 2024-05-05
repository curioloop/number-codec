# number-codec
Lightweight implementation of popular lossless numerical compression algorithms

### Support encodings
- **[Gorilla](src/main/java/com/curioloop/number/codec/gorilla/GorillaCodec.java):** Fast lossless float point number compression
- **[Chimp](src/main/java/com/curioloop/number/codec/chimp/ChimpN.java):** Adaptive lossless float point number compression
- **[VarInt](src/main/java/com/curioloop/number/codec/varint/VarInt.java):** Variable-length integer encoding for unsigned integer
- **[ZigZag](src/main/java/com/curioloop/number/codec/varint/ZigZag.java):** Variable-length integer encoding for signed integer
- **[Simple8](src/main/java/com/curioloop/number/codec/simple8/Simple8Codec.java):** Packing multiple integers into a single 64-bit word
- **[Delta2](src/main/java/com/curioloop/number/codec/delta2/Delta2Codec.java):** Store the difference between consecutive values only

### When could you benefit from it ? 
These algorithms are particularly useful in the realm of time-series data:
- Trading messages in financial markets
- IoT network data streams generated by sensors
- System metrics collected by performance monitor 

[In our case](src/test/java/com/curioloop/number/codec/test/TestCompressRate.java), the data volume can be reduced over 30% by applying these compression algorithms.
This approach effectively slashes network bandwidth consumption and curtails disk storage expenses, ultimately trimming down the company's infrastructure overheads.

### Why should you try it ?
- Pure Java implementation without any external dependency
- Efficient memory access through [Unsafe](src/test/java/com/curioloop/number/codec/test/CodecBufferPerf.java)
- Deeply optimized for [Simple8 encoding](src/main/java/com/curioloop/number/codec/simple8/FastLookup.java)

### How to use it ?
1. Import maven dependency
```xml
<project>

    <dependencies>
        <dependency>
            <groupId>com.curioloop</groupId>
            <artifactId>number-codec</artifactId>
            <version>1.1.0</version>
        </dependency>
    </dependencies>

</project>
```

2. Try the [CodecHelper](src/main/java/com/curioloop/number/codec/CodecHelper.java) or custom you own workflow (recommend) like [this](src/test/java/com/curioloop/number/codec/test/TestCompressRate.java)
```java
public static void main(String[] args) {

    CodecSlice slice = new CodecSlice();

    // Encode and decode with delta2
    CodecResult cr1 = CodecHelper.encodeDelta2(i -> i, 10000);
    CodecHelper.decodeDelta2(slice.wrap(cr1.data()), cr1.codecs(), Assertions::assertEquals);

    // Encode and decode integers
    CodecResult cr2 = CodecHelper.encodeInt(i -> i, 10000, true);
    CodecHelper.decodeInt(slice.wrap(cr2.data()), cr2.codecs(), Assertions::assertEquals);

    // Encode and decode longs
    CodecResult cr3 = CodecHelper.encodeLong(i -> i, 10000, true);
    CodecHelper.decodeLong(slice.wrap(cr3.data()), cr3.codecs(), Assertions::assertEquals);

    // Encode and decode floats
    CodecResult cr4 = CodecHelper.encodeFloat(i -> i, 10000);
    CodecHelper.decodeFloat(slice.wrap(cr4.data()), cr4.codecs(), Assertions::assertEquals);

    // Encode and decode doubles
    CodecResult cr5 = CodecHelper.encodeDouble(i -> i, 10000);
    CodecHelper.decodeDouble(slice.wrap(cr5.data()), cr5.codecs(), Assertions::assertEquals);
    
}
```
