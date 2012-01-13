package controllers.casino;


import java.lang.reflect.InvocationTargetException;
import java.util.List;

import models.casino.User;
import play.Play;
import play.utils.Java;

/**
 * A more or less exact copy of the secure implementation of play.
 * - enhanced with a transport guarantee. therefore logins are fixed to a uri eg https://example.com.
 * - enhanced with putting a session on both original and fixed domain 
 * - Secure.Security authentifies against casino.User model
 * 
 * @author ra
 *
 */
public class SecurityCasino extends SecureCasino.Security {

        /**
         * @Deprecated
         * 
         * @param username
         * @param password
         * @return
         */
        static boolean authentify(String username, String password) {
            throw new UnsupportedOperationException();
        }

        /**
    	 * Extend Play!s security mechanism to authenticate against
    	 * the User object.
    	 */
    	public static boolean authenticate(String username, String password) {

    		
    		User user = User.all().filter("email", username).get();
    		
    		
    		//the email should be there
    		if (user == null) {
    			return false;
    		}
    		
    		//make sure the user confirmed the name
    		if (user.confirmationCode.length() != 0) {
    			return false;
    		}
    		
    		

    		return user.isThisCorrectUserPassword(password);

    	}

    	/**
    	 * Annotate your methods using 
    	 * @Check("isConnected")
    	 * 
    	 * check roles with:
    	 * @Check("role:admin")
    	 * or
    	 * @Check("role:admin")
    	 * or more general:
    	 * @Check("role:ROLE") => depending on the roles you have in your app...
    	 * 
    	 * 
    	 * @param check
    	 * @return
    	 */
        public static boolean check(String check) {
        	
        	// Possibility 1: Make sure is connected
        	if ("isConnected".equals(check)) {
        		return SecurityCasino.isConnected();   		
        	}
        	
        	
        	// Possibility 2: Check for a certain role
        	if (check.startsWith("role:")) {
        		
        		
        		String email = SecurityCasino.connected();
        		//if user is not logged in role checking does not make sense...
        		if (email == null) {
        			return false;
        		}
        		
        		User user = User.all().filter("email", email).get();
        		//if user does not exist role checking does not make sense...
        		if (user == null) {
        			return false;
        		}
        		
        		String [] splittedStuff = check.split(":");
        		
        		if (splittedStuff.length > 1) {
        			
        			String role = splittedStuff[1];
        			
        			//now check if user is in that role and return result...       			
         			return user.hasRole(role);
        			
        		}
        		
        	}
        	    	
        	return false;
        }     

        /**
         * This method returns the current connected username
         * @return
         */
        public static String connected() {
            return session.get("username");
        }

        /**
         * Indicate if a user is currently connected
         * @return  true if the user is connected
         */
        public static boolean isConnected() {
            return session.contains("username");
        }

        /**
         * This method is called after a successful authentication.
         * You need to override this method if you with to perform specific actions (eg. Record the time the user signed in)
         */
        static void onAuthenticated() {
        	
        	
        	String secureUrl = Play.configuration.getProperty(CasinoConstants.secureUrl, "");
        	
        	//only do stuff if we have two domains:
        	if (!secureUrl.equals("")) {
        		
        		String regularUrl = Play.configuration.getProperty(CasinoConstants.regularUrl, "");
        		
        		if (regularUrl.equals("")) {
        			throw new RuntimeException("Error. Please set " + CasinoConstants.regularUrl + " AND " + CasinoConstants.secureUrl + " in application.conf.");
        		}
        		
        		String url = flash.get("url");
        		if (url == null) {
        			url = "/"; 
        		}
            	String username = session.get("username");
            		
            	String token = SessionTransfer.doSetMemcacheToken(username);

                //System.out.println("redirecting and authentificating cookie on other server......");                

                redirect(regularUrl + "/login/auth_via_token?token="+token+"&url="+url);          		
        	}
   	
        }

         /**
         * This method is called before a user tries to sign off.
         * You need to override this method if you wish to perform specific actions (eg. Record the name of the user who signed off)
         */
        static void onDisconnect() {
            
        	String secureUrl = Play.configuration.getProperty(CasinoConstants.secureUrl, "");
        	
        	//save username so we can inform the other server (if there is one)
        	String username = session.get("username");
        	
        	//clear stuff on this server => because we redirect the stuff in original logout won't be called...
        	session.clear();
            response.removeCookie("rememberme"); 
              
        	//only do stuff if we have two domains:
        	if (!secureUrl.equals("")) {
        		
        		String regularUrl = Play.configuration.getProperty(CasinoConstants.regularUrl, "");
        		
        		if (regularUrl.equals("")) {
        			throw new RuntimeException("Error. Please set " + CasinoConstants.regularUrl + " AND " + CasinoConstants.secureUrl + " in application.conf.");
        		}
        		
        		String url = flash.get("url");
        		if (url == null) {
        			url = "/"; 
        		}


            	String token = SessionTransfer.doSetMemcacheToken(username);
            	
                //System.out.println("redirecting and nulling cookie on other server......");                

                //I assume we are on the secureUrl. Therefore we also sign the other url...
            	redirect(regularUrl + "/logout/auth_via_token?token="+token+"&url="+url);        		
        	}
        	
        	
        }

         /**
         * This method is called after a successful sign off.
         * You need to override this method if you wish to perform specific actions (eg. Record the time the user signed off)
         */
        static void onDisconnected() {
        	

        }

        /**
         * This method is called if a check does not succeed. By default it shows the not allowed page (the controller forbidden method).
         * @param profile
         */
        static void onCheckFailed(String profile) {
            forbidden();
        }

        private static Object invoke(String m, Object... args) throws Throwable {
            Class security = null;
            List<Class> classes = Play.classloader.getAssignableClasses(SecurityCasino.class);
            if(classes.size() == 0) {
                security = SecurityCasino.class;
            } else {
                security = classes.get(0);
            }
            try {
                return Java.invokeStaticOrParent(security, m, args);
            } catch(InvocationTargetException e) {
                throw e.getTargetException();
            }
        }

    
}
