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
import com.wire.bots.github.resource.TravisResource;
import com.wire.bots.sdk.Configuration;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.Server;
import com.wire.bots.sdk.crypto.CryptoDatabase;
import com.wire.bots.sdk.crypto.storage.RedisStorage;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.RedisState;
import com.wire.bots.sdk.tools.Logger;
import io.dropwizard.setup.Environment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class Service extends Server<Config> {
    static Config config;

    public static void main(String[] args) throws Exception {
        new Service().run(args);
    }

    @Override
    protected void initialize(Config config, Environment env) {
        Service.config = config;
        env.jersey().setUrlPattern("/github/*");
    }

    @Override
    protected MessageHandlerBase createHandler(Config config, Environment env) {
        return new MessageHandler(getStorageFactory(config));
    }

    @Override
    protected void onRun(Config config, Environment env) {
        addResource(new GitHubResource(repo, new HmacValidator()), env);
        try {
            PublicKey publicKey = extractTravisPubKey();
            addResource(new TravisResource(repo, new SHA256RsaValidator(publicKey)), env);
        } catch (Exception e) {
            Logger.error("OnRun: %s", e);
            e.printStackTrace();
        }
    }

    /**
     * Instructs the framework to use Storage Service for the state.
     * Remove this override in order to use local File system storage
     *
     * @param config Config
     * @return Storage
     */
    @Override
    protected StorageFactory getStorageFactory(Config config) {
        return botId -> new RedisState(botId, config.db);
    }

    /**
     * Instructs the framework to use Crypto Service for the crypto keys.
     * Remove this override in order to store cryptobox onto your local File system
     *
     * @param config Config
     * @return CryptoFactory
     */
    @Override
    protected CryptoFactory getCryptoFactory(Config config) {
        return (botId) -> {
            Configuration.DB db = config.db;
            RedisStorage storage = new RedisStorage(db.host, db.port, db.password, db.timeout);
            return new CryptoDatabase(botId, storage);
        };
    }

    private PublicKey extractTravisPubKey()
            throws InvalidKeySpecException, NoSuchAlgorithmException, URISyntaxException, IOException {
        URL resource = getClass().getClassLoader().getResource("api.travis-ci.org/pubkey.pem");
        byte[] raw = Files.readAllBytes(Paths.get(resource.toURI()));

        String pubKeyPem = new String(raw);
        pubKeyPem = pubKeyPem.replace("\n", "");
        pubKeyPem = pubKeyPem.replace("-----BEGIN PUBLIC KEY-----", "");
        pubKeyPem = pubKeyPem.replace("-----END PUBLIC KEY-----", "");

        byte[] decoded = Base64.getDecoder().decode(pubKeyPem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
}
