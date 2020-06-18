#ifndef CLIENTDNS_DNSCLIENT_H
#define CLIENTDNS_DNSCLIENT_H

#pragma once

#include <winsock2.h>
#include <stdio.h>
#include <stdlib.h>
#include <string>
#include <cstring>
#include <winsock2.h>
#include <windows.h>
#include <ws2tcpip.h>
#include <vector>
#include <iostream>
#include <map>
#include "Response.h"

using namespace std;

static map<char*, char*> authBuffer;

class DNSClient {
private:
	int sock = 0;
	struct sockaddr_in serv_addr;
	unsigned char buffer[512] = { 0 };
	int port = 53;
	int processAnswer(char* query, unsigned int start_pos, unsigned int adit_info, unsigned int auth_info);
	void readByPointer(int addr);
	void readDataChunk(int addr, int len);
public:
	static map<std::string, std::string> authorityCache;
	vector<Response> resource_cache;
	int buffer_idx = 0;
	DNSClient(char* serverId) {

		if ((sock = socket(PF_INET, SOCK_DGRAM, 0)) < 0)
		{
			printf("\n Socket creation error \n");
		}

		memset(&serv_addr, '0', sizeof(serv_addr));

		serv_addr.sin_family = AF_INET;
		serv_addr.sin_port = htons(port);

		if (inet_pton(AF_INET, serverId /*"81.180.223.1"  "8.8.8.8"*/, &serv_addr.sin_addr) <= 0)
		{
			printf("\nInvalid address/ Address not supported \n");
		}

		if (connect(sock, (struct sockaddr*) & serv_addr, sizeof(serv_addr)) < 0)
		{
			printf("\nConnection Failed \n");
		}

	}

	~DNSClient() {
		closesocket(sock);
	}

	void sendUDPRequest(string query);
	void parseUDPResponse(char* query);

};


#endif //CLIENTDNS_DNSCLIENT_H
