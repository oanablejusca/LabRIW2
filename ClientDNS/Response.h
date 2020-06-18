#ifndef CLIENTDNS_RESPONSE_H
#define CLIENTDNS_RESPONSE_H
#include<string.h>

class Response {
public:
	char IP[16];
	unsigned int TTL;
	Response(char addr[], int ttl) {
		strcpy_s(IP, addr);
		this->TTL = ttl;
	}

};

#endif 
