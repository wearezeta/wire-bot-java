//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.github;

import com.wire.bots.github.resource.GitHubResource;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.Server;
import io.dropwizard.setup.Environment;

public class BotService extends Server<BotConfig> {
    public static void main(String[] args) throws Exception {
        new BotService().run(args);
    }

    @Override
    protected MessageHandlerBase createHandler(BotConfig config, Environment env) {
        return new MessageHandler(config);
    }

    @Override
    protected void onRun(BotConfig botConfig, Environment env) {
        Validator validator = new Validator(config.getCryptoDir());
        addResource(new GitHubResource(repo, validator), env);
    }
}
