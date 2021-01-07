/*
 * Copyright 2016-2018 Leon Chen
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

package syncer.replica.cmd.parser;


import syncer.replica.cmd.CommandParser;
import syncer.replica.cmd.CommandParsers;
import syncer.replica.cmd.impl.HDelCommand;

/**
 * @author Leon Chen
 * @since 2.1.0
 */
public class HDelParser implements CommandParser<HDelCommand> {
    @Override
    public HDelCommand parse(Object[] command) {
        int idx = 1;
        byte[] key = CommandParsers.toBytes(command[idx]);
        idx++;
        byte[][] fields = new byte[command.length - 2][];
        for (int i = idx, j = 0; i < command.length; i++, j++) {
            fields[j] = CommandParsers.toBytes(command[i]);
        }
        return new HDelCommand(key, fields);
    }

}