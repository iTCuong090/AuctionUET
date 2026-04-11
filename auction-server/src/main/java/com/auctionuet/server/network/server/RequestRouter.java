package com.auctionuet.server.network.server;

import com.auctionuet.server.network.controller.AuthController;
import com.auctionuet.server.network.protocol.Request;
import com.auctionuet.server.network.protocol.Response;

public class RequestRouter {
    private AuthController authController;
    public RequestRouter(AuthController authController){
        this.authController=authController;
    }
    public Response route(Request request){
            if (request == null || request.getAction() == null) {
                return Response.error("Invalid request: missing action");
            }

            switch (request.getAction()) {
                case LOGIN:
                    return authController.handleLogin(request);
                case REGISTER:
                    return authController.handleRegister(request);
                case LOGOUT:
                    return authController.handleLogout(request);
                case PING:
                    return Response.ok("PONG");
                default:
                    return Response.error("Unknown action: " + request.getAction());
            }
        }
    }


