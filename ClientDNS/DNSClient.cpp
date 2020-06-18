#pragma comment(lib, "Ws2_32.lib")

#include "DNSClient.h"
#include <string.h>
#include <windows.h>
#include <Ws2tcpip.h>
#include <iostream>
#include <io.h>
#include "Response.h"

void DNSClient::sendUDPRequest(string query) {
	int request_ID = rand();
	const int message_length = query.length() + 2 + 12 + 4;
    char* message = new  char[message_length];
	memset(message, 0, message_length);
	message[0] = (request_ID & 0xFF00) >> 8;
	message[1] = request_ID & 0xFF;
	// message[2] = 1;
	//nr de cereri
	message[5] = 1;

	int label_len_pos = 12; //position of the first len
	int in_package_pos = 13;
	int nr_chars = 0;

	for (int i = 0; i < query.length(); i++) {
		if (query[i] != '.') {
			message[in_package_pos] = query[i];
			nr_chars++;
		}
		else
		{
			message[label_len_pos] = nr_chars;
			label_len_pos = in_package_pos;
			nr_chars = 0;
		}
		in_package_pos++;
	}
	message[label_len_pos] = nr_chars;
	message[in_package_pos + 1] = 0;
	message[in_package_pos + 2] = 1;
	message[in_package_pos + 3] = 0;
	message[in_package_pos + 4] = 1;

	/*for(int i = 0; i<message_length; i++) {
		printf("%x ", message[i]);
	}
	printf("\n");
	*/
	send(sock, message, message_length, 0);
	// printf("Message sent\n");
	_read(sock, buffer, 512);
	/*for(int i = 0; i<100; i++) {
		printf("%x ", buffer[i]);
	}
	std::cout << std::endl;
	 */
}

void DNSClient::parseUDPResponse(char* query) {
	resource_cache.clear();
	// citeste al 3 lea byte..ia partea cea mai putin semnificativa
	unsigned char resp_code = buffer[3] & 0x0F;
	//daca codul e diferit de 0..error code
	if (resp_code != 0) {
		// std::cout << "err_code: "<< (unsigned int)resp_code << std::endl;
		return;
	}

	// calcul queries
	unsigned int queries_count = (buffer[4] << 8) | buffer[5];
	//std::cout <<" # of queries: " << queries_count << std::endl;

	//calcul response count
	unsigned int response_count = (buffer[6] << 8) | buffer[7];
	std::cout << " # of responses: " << response_count << std::endl;

	unsigned int auth_count = (buffer[8] << 8) | buffer[9];
	std::cout << "Auth count: " << auth_count << std::endl;

	unsigned int adit_info = (buffer[10] << 8) | buffer[11];
	std::cout << "Aditional info: " << adit_info << std::endl;


	// citire Question pana dau de un /0
	//std::cout << "Question " <<" :";
	int i = 12;
	while (buffer[i] != 0) {
		printf("%x ", buffer[i]);
		i++;
	}
	i++; //pass the 0 byte for question ending
	cout << endl;

	unsigned int type = (buffer[i] << 8) | buffer[i + 1];
	//cout << "Type: "<< type << std::endl;
	i += 2;
	unsigned int cls = (buffer[i] << 8) | buffer[i + 1];
	// cout << "Class: " << cls << endl;
	i += 2;
	cout << " ------Answer:------ " << endl;
	for (int q = 0; q < response_count; q++) {
		i = processAnswer(query, i, adit_info, auth_count);
	}
	if (auth_count > 0 || adit_info > 0)
	{
		cout << "----AUTH-----" << endl;
		for (int a = 0; a < auth_count && i > 0 && i < 512; a++) {
			i = processAnswer(query, i, 0, 0);
		}
		if (i > 0) {
			printf("\n------Additional info ------\n");
			for (int a = 0; a < adit_info && i > 0 && i < 512; a++) {
				i = processAnswer(query, i, 0, 0);
			}
		}

	}

}
void DNSClient::readByPointer(int addr) {
	int i = addr;
	//citire bite cu bite..daca e mai mare decat 192..calculeza valoarea pointerului
	while (buffer[i] != 0 && i < 512) {
		if (buffer[i] < 192) {
			if ((buffer[i] >= 'a' && buffer[i] <= 'z') || (buffer[i] >= '1' && buffer[i] <= '9'))
				printf("%c", buffer[i]);
			else
				printf(".");

			i++;
		}
		else {
			int new_i = ((buffer[i] & 0x3F) << 6) | buffer[i + 1];
			readByPointer(new_i);
			return;
		}
	}
}
void DNSClient::readDataChunk(int addr, int len) {
	if (addr + len > 512)
	{
		return;
	}
	int i = addr;
	while ((i - addr) < len) {
		if (buffer[i] < 192) {
			// printf("0x%02x ", buffer[i]);
			if ((buffer[i] >= 'a' && buffer[i] <= 'z') || (buffer[i] >= '1' && buffer[i] <= '9'))
				printf("%c", buffer[i]);
			else
				printf(".");
			i++;
		}
		else {
			int new_i = ((buffer[i] & 0x3F) << 6) | buffer[i + 1];
			readByPointer(new_i);
			i += 2;
		}
	}
}

int DNSClient::processAnswer(char* query, unsigned int start_pos, unsigned int adit_info, unsigned int auth_info) {
	int i = start_pos;
	while (buffer[i] != 0) {
		if (buffer[i] < 192) {
			//printf("0x%02x ", buffer[i]);
			if ((buffer[i] >= 'a' && buffer[i] <= 'z') || (buffer[i] >= '1' && buffer[i] <= '9'))
				cout << (char)buffer[i] << " ";
			else
				cout << (int)buffer[i] << " ";
			i++;
		} // inseamna ca e pointer-> regula de constructie
		else {
			int new_i = ((buffer[i] & 0x3F) << 6) | buffer[i + 1];
			readByPointer(new_i);
			i += 2;
			break; // just a single question..so we can break here
		}
	}
	cout << endl;

	unsigned int type = (buffer[i] << 8) | buffer[i + 1];
	// cout << "Type: "<< type << std::endl;
	i += 2;
	unsigned int cls = (buffer[i] << 8) | buffer[i + 1];
	// cout << "Class: " << cls << endl;
	i += 2;

	int k = 0;
	unsigned int ttl = 0;
	while (k < 3) {
		ttl |= buffer[i + k];
		ttl <<= 8;
		k++;
	}
	ttl |= buffer[i + k];
	// cout << "TTL: " << ttl <<endl;

	i += k + 1;
	unsigned int response_length = (buffer[i] << 8) | buffer[i + 1];
	// cout << "Resource Data length: " << response_length << endl;
	i += 2;

	char* IP = new char[15];
	if (i + response_length > 512) {
		return -1;
	}

	switch (type) {
	case 1:
		cout << "IPv4: " << endl;
		char temp[4];
		for (int k = 0; k < response_length; k++) {
			printf("%d.", buffer[i + k]);
			sprintf_s(temp, "%d.", buffer[i + k]);
			strcat_s(IP, strlen(temp), temp);
		}
		resource_cache.push_back(Response(IP, ttl));


		buffer_idx++;
		if (auth_info == 0 && authBuffer.find(query) == authBuffer.end()) {
			authBuffer[query] = IP;
		}
		delete[](IP);
		//IPBuffer[strlen(IPBuffer) - 1] = '\n';
		cout << endl;
		//readDataChunk(i, response_length);
		i += response_length;
		break;
	case 2:
		cout << "Name server Record" << endl;
		/*for(int k = 0; k<response_length; k++) {
			printf("0x%02x ", buffer[i+k]);
		}*/
		readDataChunk(i, response_length);
		cout << endl;
		i += response_length;
		break;
	case 5:
		cout << " CAnonical Name" << endl;
		readDataChunk(i, response_length);
		cout << endl;
		i += response_length;
		break;
	case 28:
		cout << "IPV6" << endl;
		for (int k = 0; k < response_length; k++) {
			printf("%d.", buffer[i + k]);
		}
		i += response_length;
		cout << endl;
		break;
	default:
		return -1;

	}
	return i;
}

