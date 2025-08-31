package dev.mars.apex.demo.model;

/*
 * Copyright 2025 Mark Andrew Ray-Smith Cityline Ltd
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


/**
 * TradeB Confirmation model representing the root element of a trade confirmation message
 *
 * @author Mark Andrew Ray-Smith Cityline Ltd
 * @since 2025-08-28
 * @version 1.0
 */
public class TradeConfirmation {
    
    private Header header;
    private TradeB trade;

    public TradeConfirmation() {
    }

    public TradeConfirmation(Header header, TradeB trade) {
        this.header = header;
        this.trade = trade;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public TradeB getTrade() {
        return trade;
    }

    public void setTrade(TradeB trade) {
        this.trade = trade;
    }

    @Override
    public String toString() {
        return "TradeConfirmation{" +
                "header=" + header +
                ", trade=" + trade +
                '}';
    }
}
