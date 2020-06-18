#define WIN32_LEAN_AND_MEAN
#define _WINSOCK_DEPRECATED_NO_WARNINGS 

#include <iostream>
#include <windows.h>
#include <winsock2.h>
#include <Ws2tcpip.h>
#include <stdlib.h>
#include <string.h>
#include <io.h>
#include "DNSClient.h"

#define  PORT  27015
//2 socketuri:unul pe cereri si unul  de pe care se raspunde la cereri
int main() {
	int server_fd, new_socket, valread;
	int opt = 1;
	struct sockaddr_in address;
	int addrlen = sizeof(address);
	unsigned char buffer[1024] = { 0 };

	if ((server_fd = socket(AF_INET, SOCK_STREAM, 0)) == 0) {
		cout << "Unable to create the socket. Exiting.... " << endl;
		cin >> opt;
		return 0;

		if (setsockopt(server_fd, SOL_SOCKET, SO_REUSEADDR,
			(const char*)&opt, sizeof(opt))) {
			cout << " Socket error. Exiting... " << endl;
			cin >> opt;
			return 0;

		}
	}
	else {

		address.sin_family = AF_INET;
		address.sin_addr.s_addr = inet_addr("127.0.0.1");
		address.sin_port = htons(PORT);

		if (bind(server_fd, (struct sockaddr*) & address, sizeof(address)) < 0) {
			cout << "Error bind. Exiting..." << endl;
			cin >> opt;
			return 0;
		}

		if (listen(server_fd, 3) < 0)
		{
			cin >> opt;
			cout << "Error listen. Exiting..." << endl;
		}

		/* DNSClient dns;
		 dns.sendUDPRequest("www.tuiasi.ro");
		 dns.parseUDPResponse();*/

		for (;;) {
			for (int k = 0; k < 1024; k++) {
				buffer[k] = 0;
			}
			if ((new_socket = accept(server_fd, (struct sockaddr*) & address,
				(socklen_t*)&addrlen)) < 0) {
				cout << "Error accept: " << endl;

			}
			else {
				int buff_idx = 0;
				while (buff_idx < 1024 && 1 == _read(new_socket, &buffer[buff_idx], 1)) {
					if (buff_idx > 0 && buffer[buff_idx] == '\n' && buffer[buff_idx - 1] == '\r') {
						valread = 1;
						break;
					}
					buff_idx++;
				}

				if (valread > 0) {
					int no_char = (buffer[0] - '0') * 10 + buffer[1] - '0';
					char* query = new char[no_char + 1];
					for (int i = 0; i < no_char; i++) {
						query[i] = buffer[i + 2];
					}
					query[no_char] = '\0';
					printf("Query: %d -> %s\n", no_char, query);
					char* IP = nullptr;
					try {
						if (authBuffer.find(query) != authBuffer.end()) {
							IP = authBuffer[query];
						}

						if (IP == nullptr) {
							IP = new char[strlen("8.8.8.8") + 1];
							strcpy_s(IP, sizeof(IP), "8.8.8.8");
						}

						DNSClient dns(IP);
						dns.sendUDPRequest(query);
						dns.parseUDPResponse(query);
						if (dns.resource_cache.size() > 0) {
							dns.resource_cache[0].IP[strlen(dns.resource_cache[0].IP) - 1] = '\0';
							char responese[100];
							sprintf_s(responese, "%s-%d", dns.resource_cache[0].IP, dns.resource_cache[0].TTL);

							printf("IP_r : %s\n", responese);

							send(new_socket, responese, strlen(responese), 0);
							dns.buffer_idx = 0;
						}
						else {
							send(new_socket, "Error\r\n", 6, 0);
						}
					}
					catch (char* ex) {
						printf("%s\n", ex);
					}
					//}
					// printf("%s\n", buffer);
	//                if (dns.resource_cache.size() > 0) {
	//                    dns.resource_cache[0].IP[strlen(dns.resource_cache[0].IP) - 1] = '\0';
	//                    char responese[100];
	//                    sprintf(responese, "%s-%d", dns.resource_cache[0].IP, dns.resource_cache[0].TTL);
	//                    send(new_socket, responese, strlen(responese), 0);
	//                    dns.buffer_idx = 0;
	//                } else {
	//                    send(new_socket, "Error\n", 6, 0);
	//                }
				}
				closesocket(new_socket);
			}

		}
	}
	
}