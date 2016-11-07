package snmpwalk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

public class snmp {
	private static int maxRepetitions = 1000;//最大返回数
	private static String OID_head = "1.3.6.1.2.1.17.4.3.1.";//OID头部
	public static void main(String[] args) throws Exception {
		String elements[] = new String[4];
		boolean inputCheck = false;
		while (!inputCheck) {
			String str = readDataFromConsole();
			inputCheck = checkInput(str, elements);
		}
		Map<String, FDB> group = new HashMap<String, FDB>();
		for (int identifier = 1; identifier <= 2; identifier++) {
			try {
				CommunityTarget myTarget = new CommunityTarget();
				Address deviceAdd = GenericAddress.parse("udp:"+ elements[3] +"/161");
				myTarget.setAddress(deviceAdd);
				myTarget.setCommunity(new OctetString(elements[1]));
				myTarget.setRetries(2);
				myTarget.setTimeout(5 * 60);
				if(elements[2].equals("2c")){
					myTarget.setVersion(SnmpConstants.version2c);
				} else if (elements[2].equals("1")) {
					myTarget.setVersion(SnmpConstants.version1);
				} else {
					myTarget.setVersion(SnmpConstants.version3);
				}

				@SuppressWarnings("rawtypes")
				TransportMapping transport = new DefaultUdpTransportMapping();
				transport.listen();
				@SuppressWarnings("unchecked")
				Snmp protocol = new Snmp(transport);
				PDU request = new PDU();
				String ID = OID_head + identifier;
				request.add(new VariableBinding(new OID(ID)));
				request.setType(PDU.GETBULK);
				request.setMaxRepetitions(maxRepetitions);
				ResponseEvent responseEvent = protocol.send(request, myTarget, transport);
				PDU response = responseEvent.getResponse();
				if (response != null) {
					for (int i = 0; i < response.size(); i++) {
						VariableBinding vb1 = response.get(i);
						String s = vb1.toString();
						s = "*." + s.substring(23, s.length());
						String str[] = s.split(" = ");
						if(group.containsKey(str[0])){
							if(identifier==1){//Mac
								group.get(str[0]).setMac(str[1]);
							}else{//Port
								group.get(str[0]).setPort(str[1]);
							}
						} else {
							if(identifier==1){//Mac
								FDB data = new FDB();
								data.setMac(str[1]);
								group.put(str[0], data);
							}else{//Port
								FDB data = new FDB();
								data.setPort(str[1]);
								group.put(str[0], data);
							}
						}
					}
					transport.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		printResult(group);
	}
	
	private static void printResult(Map<String, FDB> group) {  
		System.out.println("Total records = " + group.size());
		System.out.println("===========iso======================mac==========port=");
		for (String head : group.keySet()) {
			String head_format = head;
			if(head_format.length()<26){
				while (head_format.length()<26) {
					head_format += " ";
				}
			}
			FDB fdb = group.get(head);
			if(fdb.getMac()!="null")
				System.out.println(head_format + " | " + fdb.getMac() + " |  " + fdb.getPort());
			else {
				System.out.println(head_format + " |                   |  " + fdb.getPort());
			}
		}
		System.out.println("===========iso======================mac==========port=");
    }
	
	private static String readDataFromConsole() {  
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));  
        String str = null;  
        try {  
            System.out.println("PLEASE INPUT COMMAND");
            System.out.println("example：snmpwalk -c broadapublic -v2c 10.1.1.51");
            System.out.println("======================>");
            str = br.readLine();  
  
        } catch (IOException e) {
            e.printStackTrace();  
        }  
        return str;  
    }
    
	private static boolean checkInput(String str, String elements[]) throws Exception {
		try {
			String strFormat = str.replaceAll(" ", "");
			// command elements[0]
			if (!strFormat.substring(0, 8).equals("snmpwalk")) {
				System.err.println("can't find command: " + str.split(" ")[0]);
				return false;
			} else {
				elements[0] = "snmpwalk";
			}
			// community elements[1]
			if (!strFormat.split("-")[1].substring(0, 1).equals("c")) {
				System.err.println("can't find element: -" + strFormat.split("-")[1].substring(0, 1));
				return false;
			} else {
				elements[1] = strFormat.split("-")[1].substring(1, strFormat.split("-")[1].length());
			}
			// version elements[2]
			if (!strFormat.split("-")[2].substring(0, 1).equals("v")) {
				System.err.println("can't find element: -" + strFormat.split("-")[2].substring(0, 1));
				return false;
			} else {
				if (strFormat.split("-")[2].contains("c")) {
					if (!strFormat.split("-")[2].substring(1, 3).equals("2c")) {
						return false;
					}
					elements[2] = "2c";
				} else {
					elements[2] = strFormat.split("-")[2].substring(1, 2);
				}
			}
			// IP elements[3]
			while (str.contains("  ")) {
				str.replaceAll("  ", " ");
			}
			if (!str.split(" ")[str.split(" ").length - 1].matches(
					"([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}")) {
				System.err.println("can't find command: " + str.split(" ")[str.split(" ").length - 1]);
				return false;
			} else {
				elements[3] = str.split(" ")[str.split(" ").length - 1];
			}
			return true;
		} catch (Exception e) {
			System.err.println("typing error");
		}
		return false;
	}
}
