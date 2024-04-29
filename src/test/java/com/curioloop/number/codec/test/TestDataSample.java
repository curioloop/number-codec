/*
 * Copyright © 2024 CurioLoop (curioloops@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.curioloop.number.codec.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

public class TestDataSample {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KLine {

        public static final int BYTES =
                Long.BYTES * 2 + Float.BYTES * 4 + Double.BYTES;

        long time, volume;
        float open, close, high, low;
        double amount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trade {

        public static final int BYTES =
                Long.BYTES + Float.BYTES + Integer.BYTES;

        long time;
        float price;
        int size;
    }

    @SneakyThrows
    static void loadData(String file, Consumer<String> loader) {
        URL sample = Thread.currentThread().getContextClassLoader().getResource("sample");
        Path path = Paths.get(sample.getPath()).resolve(file);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                loader.accept(line);
            }
        }
    }

    static Map<String, List<KLine>> loadKline(String file) {
        Map<String, List<KLine>> data = new TreeMap<>();
        loadData(file, line -> {
            String[] fields = line.split(",");
            String symbol = fields[0];
            long time = Long.parseLong(fields[1]);
            float open = Float.parseFloat(fields[2]);
            float close = Float.parseFloat(fields[3]);
            float high = Float.parseFloat(fields[4]);
            float low = Float.parseFloat(fields[5]);
            long volume = Long.parseLong(fields[1]);
            double amount = Double.parseDouble(fields[7]);
            List<KLine> group = data.computeIfAbsent(symbol, k -> new ArrayList<>(100));
            group.add(new KLine(time, volume, open, close, high, low, amount));
        });
        return data;
    }

    static Map<String, List<Trade>> loadTrade(String file) {
        Map<String, List<Trade>> data = new TreeMap<>();
        loadData(file, line -> {
            String[] fields = line.split(",");
            String symbol = fields[0];
            long time = Long.parseLong(fields[1]);
            float price = Float.parseFloat(fields[2]);
            int size = Integer.parseInt(fields[3]);
            List<Trade> group = data.computeIfAbsent(symbol, k -> new ArrayList<>(500));
            group.add(new Trade(time, price, size));
        });
        return data;
    }

}
