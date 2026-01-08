package dev.marketplace.marketplace.resolvers;

 import dev.marketplace.marketplace.dto.AuthResponseDto;
 import dev.marketplace.marketplace.model.User;
 import dev.marketplace.marketplace.security.JwtUtil;
 import dev.marketplace.marketplace.service.UserService;
 import graphql.schema.DataFetchingEnvironment;
 import graphql.GraphQLContext;
 import jakarta.servlet.http.HttpServletRequest;
 import jakarta.servlet.http.HttpServletResponse;
 import org.springframework.graphql.data.method.annotation.Argument;
 import org.springframework.graphql.data.method.annotation.MutationMapping;
 import org.springframework.stereotype.Controller;
 import dev.marketplace.marketplace.exceptions.AuthException;
 import dev.marketplace.marketplace.exceptions.UserAlreadyExistsException;
 import dev.marketplace.marketplace.exceptions.InvalidCredentialsException;
 import dev.marketplace.marketplace.exceptions.ValidationException;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

 import java.util.Optional;
 import java.util.UUID;
 import org.springframework.web.context.request.RequestContextHolder;
 import org.springframework.web.context.request.ServletRequestAttributes;

 @Controller
 public class AuthResolver {
     private static final Logger logger = LoggerFactory.getLogger(AuthResolver.class);

     private final UserService userService;
     private final JwtUtil jwtUtil;

     public AuthResolver(UserService userService, JwtUtil jwtUtil) {
         this.userService = userService;
         this.jwtUtil = jwtUtil;
     }

     // Helper: get servlet response from GraphQL context or fallback to RequestContextHolder
     private HttpServletResponse resolveResponse(GraphQLContext graphQLContext) {
         try {
             if (graphQLContext != null) {
                 Object obj = graphQLContext.get("jakarta.servlet.http.HttpServletResponse");
                 if (obj instanceof HttpServletResponse) {
                     logger.debug("AuthResolver - obtained HttpServletResponse from GraphQL context");
                     return (HttpServletResponse) obj;
                 }
             }
         } catch (Exception e) {
             logger.debug("AuthResolver - exception reading response from GraphQL context: {}", e.getMessage());
         }

         // Fallback to RequestContextHolder
         var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
         if (attrs != null) {
             logger.debug("AuthResolver - obtained HttpServletResponse from RequestContextHolder fallback");
             return attrs.getResponse();
         }
         logger.debug("AuthResolver - could not resolve HttpServletResponse from context");
         return null;
     }

     // Helper: get servlet request from GraphQL context or fallback to RequestContextHolder
     private HttpServletRequest resolveRequest(GraphQLContext graphQLContext) {
         try {
             if (graphQLContext != null) {
                 Object obj = graphQLContext.get("jakarta.servlet.http.HttpServletRequest");
                 if (obj instanceof HttpServletRequest) {
                     logger.debug("AuthResolver - obtained HttpServletRequest from GraphQL context");
                     return (HttpServletRequest) obj;
                 }
             }
         } catch (Exception e) {
             logger.debug("AuthResolver - exception reading request from GraphQL context: {}", e.getMessage());
         }

         var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
         if (attrs != null) {
             logger.debug("AuthResolver - obtained HttpServletRequest from RequestContextHolder fallback");
             return attrs.getRequest();
         }
         logger.debug("AuthResolver - could not resolve HttpServletRequest from context");
         return null;
     }

     @MutationMapping
     public AuthResponseDto register(@Argument String username, @Argument String email, @Argument String password, DataFetchingEnvironment env) {
         GraphQLContext graphQLContext = env.getGraphQlContext();
         HttpServletResponse response = resolveResponse(graphQLContext);
         // Resolve request early so we can compute cookie attributes even if response is null
         HttpServletRequest servletRequestForAttrs = resolveRequest(graphQLContext);
         logger.info("Registration attempt - Username: {}, Email: {}", username, email);
         try {
             User user = userService.registerUser(username, email, password);
             String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
             logger.info("Registration successful for user: {}", user.getEmail());

             // Compute cookie attributes based on the request we resolved (if any)
             boolean isSecure = false;
             boolean isLocalhost = false;
             if (servletRequestForAttrs != null) {
                 isSecure = servletRequestForAttrs.isSecure() || "https".equalsIgnoreCase(servletRequestForAttrs.getHeader("X-Forwarded-Proto"));
                 String host = servletRequestForAttrs.getServerName();
                 isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
             }
             // Only set SameSite=None when cookie will also be Secure (addSecure=true). For localhost or non-secure requests use Lax.
             boolean addSecure = isSecure && !isLocalhost;
             String sameSite = addSecure ? "None" : "Lax";

             // Build Set-Cookie header strings (log them regardless of whether we have a response to write to)
             StringBuilder authSb = new StringBuilder();
             authSb.append("auth-token=").append(token);
             authSb.append("; Path=/");
             authSb.append("; Max-Age=").append(7 * 24 * 60 * 60);
             authSb.append("; HttpOnly");
             if (addSecure) authSb.append("; Secure");
             authSb.append("; SameSite=").append(sameSite);
             String authHeaderOut = authSb.toString();
             logger.debug("AuthResolver - Built Set-Cookie for auth-token: {}", authHeaderOut);

             String devHeader = "auth-token-dev=" + token + "; Path=/; Max-Age=" + (7 * 24 * 60 * 60) + "; SameSite=Lax";
             logger.debug("AuthResolver - Built Set-Cookie for auth-token-dev: {}", devHeader);

             String csrfToken = UUID.randomUUID().toString();
             String csrfHeader = "XSRF-TOKEN=" + csrfToken + "; Path=/; Max-Age=" + (7 * 24 * 60 * 60) + "; SameSite=" + sameSite;
             if (addSecure) csrfHeader += "; Secure";
             logger.debug("AuthResolver - Built Set-Cookie for XSRF-TOKEN: {}", csrfHeader);
             response.addHeader("Set-Cookie", csrfHeader);
             // Also expose the CSRF token in a response header for frontend dev usage
             response.addHeader("X-XSRF-TOKEN", csrfToken);

             // Now add headers to response if available
             if (response != null) {
                 response.addHeader("Set-Cookie", authHeaderOut);
                 if (isLocalhost) {
                     response.addHeader("Set-Cookie", devHeader);
                     response.addHeader("X-DEV-AUTH-TOKEN", token);
                 }
                 response.addHeader("Set-Cookie", csrfHeader);

                 String origin = servletRequestForAttrs != null ? servletRequestForAttrs.getHeader("Origin") : null;
                 if (origin != null && origin.contains("localhost")) {
                     response.setHeader("Access-Control-Allow-Origin", origin);
                     response.setHeader("Access-Control-Allow-Credentials", "true");
                     response.setHeader("Access-Control-Expose-Headers", "X-DEV-AUTH-TOKEN, X-XSRF-TOKEN");
                 }
             } else {
                 logger.warn("AuthResolver - HttpServletResponse is null; cookie headers were built but not added to response");
             }

             return new AuthResponseDto(token, user.getEmail(), user.getRole().name(), user.getId());
         } catch (UserAlreadyExistsException | ValidationException e) {
             logger.warn("Registration failed with known exception: {}", e.getMessage());
             throw e;
         } catch (Exception e) {
             logger.error("Registration failed with unexpected error: {}", e.getMessage(), e);
             throw new AuthException("Registration failed. Please try again.");
         }
     }

     @MutationMapping
     public AuthResponseDto login(@Argument String emailOrUsername, @Argument String password, DataFetchingEnvironment env) {
         GraphQLContext graphQLContext = env.getGraphQlContext();
         HttpServletResponse response = resolveResponse(graphQLContext);
         HttpServletRequest servletRequestForAttrs = resolveRequest(graphQLContext);
         logger.info("Login attempt - EmailOrUsername: {}", emailOrUsername);
         try {
             if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
                 logger.warn("Login failed: Email or username is empty");
                 throw new ValidationException("Email or username is required");
             }
             if (password == null || password.trim().isEmpty()) {
                 logger.warn("Login failed: Password is empty");
                 throw new ValidationException("Password is required");
             }
             logger.debug("Attempting to authenticate user: {}", emailOrUsername);
             Optional<User> userOpt = userService.authenticateUser(emailOrUsername, password);
             if (userOpt.isEmpty()) {
                 logger.warn("Login failed: No user found or invalid password for: {}", emailOrUsername);
                 throw new InvalidCredentialsException("Invalid email/username or password");
             }
             User user = userOpt.get();
             logger.info("Login successful for user: {} (ID: {})", user.getEmail(), user.getId());
             String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());

             boolean isSecure = false;
             boolean isLocalhost = false;
             if (servletRequestForAttrs != null) {
                 isSecure = servletRequestForAttrs.isSecure() || "https".equalsIgnoreCase(servletRequestForAttrs.getHeader("X-Forwarded-Proto"));
                 String host = servletRequestForAttrs.getServerName();
                 isLocalhost = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host);
             }
             boolean addSecure = isSecure && !isLocalhost;
             String sameSite = addSecure ? "None" : "Lax";

             StringBuilder authSb2 = new StringBuilder();
             authSb2.append("auth-token=").append(token);
             authSb2.append("; Path=/");
             authSb2.append("; Max-Age=").append(7 * 24 * 60 * 60);
             authSb2.append("; HttpOnly");
             if (addSecure) authSb2.append("; Secure");
             authSb2.append("; SameSite=").append(sameSite);
             String authHeaderOut2 = authSb2.toString();
             logger.debug("AuthResolver - Built Set-Cookie for auth-token: {}", authHeaderOut2);

             String devHeader2 = "auth-token-dev=" + token + "; Path=/; Max-Age=" + (7 * 24 * 60 * 60) + "; SameSite=Lax";
             logger.debug("AuthResolver - Built Set-Cookie for auth-token-dev: {}", devHeader2);

             String csrfToken = UUID.randomUUID().toString();
             String csrfHeader2 = "XSRF-TOKEN=" + csrfToken + "; Path=/; Max-Age=" + (7 * 24 * 60 * 60) + "; SameSite=" + sameSite;
             if (addSecure) csrfHeader2 += "; Secure";
             logger.debug("AuthResolver - Built Set-Cookie for XSRF-TOKEN: {}", csrfHeader2);
             response.addHeader("Set-Cookie", csrfHeader2);
             // Also expose the CSRF token in a response header for frontend dev usage
             response.addHeader("X-XSRF-TOKEN", csrfToken);

             if (response != null) {
                 response.addHeader("Set-Cookie", authHeaderOut2);
                 if (isLocalhost) {
                     response.addHeader("Set-Cookie", devHeader2);
                     response.addHeader("X-DEV-AUTH-TOKEN", token);
                 }
                 response.addHeader("Set-Cookie", csrfHeader2);

                 String origin = servletRequestForAttrs != null ? servletRequestForAttrs.getHeader("Origin") : null;
                 if (origin != null && origin.contains("localhost")) {
                     response.setHeader("Access-Control-Allow-Origin", origin);
                     response.setHeader("Access-Control-Allow-Credentials", "true");
                     response.setHeader("Access-Control-Expose-Headers", "X-DEV-AUTH-TOKEN, X-XSRF-TOKEN");
                 }
             } else {
                 logger.warn("AuthResolver - HttpServletResponse is null; cookie headers were built but not added to response");
             }

             return new AuthResponseDto(token, user.getEmail(), user.getRole().name(), user.getId());
         } catch (ValidationException | InvalidCredentialsException e) {
             logger.warn("Login failed with known exception: {}", e.getMessage());
             throw e;
         } catch (Exception e) {
             logger.error("Login failed with unexpected error: {}", e.getMessage(), e);
             throw new AuthException("Login failed. Please try again.");
         }
     }

 }
