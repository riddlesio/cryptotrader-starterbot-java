/*
 *  Copyright 2018 riddles.io (developers@riddles.io)
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 *      For the full copyright and license information, please view the LICENSE
 *      file that was distributed with this source code.
 */

package bot;

import java.util.ArrayList;
import java.util.HashMap;

import move.Move;
import move.MoveType;
import move.Order;

/**
 * bot.BotStarter - Created on 15-2-18
 *
 * Magic happens here. You should edit this file, or more specifically
 * the doMove() method to make your bot do more than random moves.
 *
 * @author Jim van Eeden - jim@riddles.io
 */
public class BotStarter {

    /**
     * Simple Moving Average Crossover
     * @param state The current state of the game
     * @return A Move object
     */
    Move makeTrades(BotState state) {
        double treshold = 0.01;  // 1% treshold for buying or selling
        return SimpleMovingAverageCrossover(state, treshold);

//        String symbol = "ETH";
//        return BuyAndHODL(state, symbol);
    }

    /**
     * Buys given symbol at the first request and holds it.
     * @param state Current state of the bot.
     * @param symbol Symbol to buy.
     * @return A move for this round, might be no move at all.
     */
    private Move BuyAndHODL(BotState state, String symbol) {
        ArrayList<Order> orders = new ArrayList<>();

        state.getCharts().forEach((pair, chart) -> {
            if (!pair.equals("USDT_" + symbol)) return;

            double dollars = state.getStacks().get("USDT");
            double price = chart.getCandleAt(state.getDate()).getClose();
            double amount = getAmount(MoveType.BUY, pair, state.getStacks(), price);

            if (amount > 0 && dollars > 0) {
                orders.add(new Order(MoveType.BUY, pair, amount));
            }
        });

        return new Move(orders);
    }

    /**
     * This is an example trading strategy that works decently well:
     * the Simple Moving Average Crossover. We compare a leading and lagging SMA for
     * all the available charts and buy if above, and sell if below given treshold.
     * @param state Current state of the bot.
     * @param treshold Treshold for a buy or sell order.
     * @return A move for this round, might be no move at all.
     */
    private Move SimpleMovingAverageCrossover(BotState state, double treshold) {
        ArrayList<Order> orders = new ArrayList<>();

        state.getCharts().forEach((pair, chart) -> {
            double SMA2 = chart.SMA(2, state.getDate(), state.getCandleInterval());
            double SMA10 = chart.SMA(10, state.getDate(), state.getCandleInterval());
            double price = chart.getCandleAt(state.getDate()).getClose();

            double diff = (SMA2 - SMA10) / price;  // get relative difference

            MoveType moveType;
            if (diff > treshold) {  // Buy if above treshold
                moveType = MoveType.BUY;
            } else if (diff < -treshold) {  // Sell if below treshold
                moveType = MoveType.SELL;
            } else {
                return;
            }

            // Get half of current stack
            double amount = getAmount(moveType, pair, state.getStacks(), price) / 2;

            if (amount <= 0) return;

            orders.add(new Order(moveType, pair, amount));
        });

        return new Move(orders);
    }

    /**
     * Gets the correct amount from the bot's stack for an order
     * @param type Buy or Sell.
     * @param pair Pair that that the order will be on.
     * @param stacks The bot's stacks.
     * @param price Price for the given pair.
     * @return An amount from the bot's stack
     */
    private double getAmount(MoveType type, String pair, HashMap<String, Double> stacks, double price) {
        String[] split = pair.split("_");

        double amount;
        if (type == MoveType.SELL) {
            amount = stacks.get(split[1]);  // Second part of pair for selling
        } else {
            amount = stacks.get(split[0]) / price;  // First part of pair for buying
        }

        if (amount < 0.00001) {  // We don't want to order on very small amounts.
            return 0;
        }

        return amount;
    }

    public static void main(String[] args) {
        BotParser parser = new BotParser(new BotStarter());
        parser.run();
    }
}
