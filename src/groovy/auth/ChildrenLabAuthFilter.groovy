package auth

import com.odobo.grails.plugin.springsecurity.rest.RestAuthenticationToken
import com.odobo.grails.plugin.springsecurity.rest.credentials.CredentialsExtractor
import com.odobo.grails.plugin.springsecurity.rest.token.generation.TokenGenerator
import com.odobo.grails.plugin.springsecurity.rest.token.storage.TokenStorageService
import groovy.util.logging.Slf4j
import org.springframework.security.authentication.AuthenticationDetailsSource
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.web.filter.GenericFilterBean

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * Created by Jay on 5/27/2015.
 */
@Slf4j
class ChildrenLabAuthFilter extends GenericFilterBean {

    CredentialsExtractor credentialsExtractor

    String endpointUrl

    AuthenticationManager authenticationManager

    AuthenticationSuccessHandler authenticationSuccessHandler
    AuthenticationFailureHandler authenticationFailureHandler

    AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource

    TokenGenerator tokenGenerator
    TokenStorageService tokenStorageService

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = request as HttpServletRequest
        HttpServletResponse httpServletResponse = response as HttpServletResponse

        def actualUri =  httpServletRequest.requestURI - httpServletRequest.contextPath

        log.debug "Actual URI is ${actualUri}; endpoint URL is ${endpointUrl}"

        //Only apply filter to the configured URL
        if (actualUri == endpointUrl) {
            log.debug "Applying authentication filter to this request"

            //Only POST is supported
            if (httpServletRequest.method != 'POST') {
                log.debug "${httpServletRequest.method} HTTP method is not supported. Setting status to ${HttpServletResponse.SC_METHOD_NOT_ALLOWED}"
                httpServletResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
                return
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication()
            Authentication authenticationResult

            UsernamePasswordAuthenticationToken authenticationRequest = credentialsExtractor.extractCredentials(httpServletRequest)

            boolean authenticationRequestIsCorrect = (authenticationRequest?.principal && authenticationRequest?.credentials)

            if(authenticationRequestIsCorrect){
                authenticationRequest.details = authenticationDetailsSource.buildDetails(httpServletRequest)

                try {
                    log.debug "Trying to authenticate the request"
                    authenticationResult = authenticationManager.authenticate(authenticationRequest)

                    if (authenticationResult.authenticated) {
                        log.debug "Request authenticated. Storing the authentication result in the security context"
                        log.debug "Authentication result: ${authenticationResult}"

                        SecurityContextHolder.context.setAuthentication(authenticationResult)
                    }
                } catch (AuthenticationException ae) {

                    log.debug "Authentication failed: ${ae.message}"
                    authenticationFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, ae)
                }

            }else{
                log.debug "Username and/or password parameters are missing."
                if(!authentication){
                    log.debug "Setting status to ${HttpServletResponse.SC_BAD_REQUEST}"
                    httpServletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST)
                    return
                }else{
                    log.debug "Using authentication already in security context."
                    authenticationResult = authentication
                }
            }

            if (authenticationResult?.authenticated) {
                String tokenValue = tokenGenerator.generateToken(authenticationResult.principal)
                log.debug "Generated token: ${tokenValue}"

                tokenStorageService.storeToken(tokenValue, authenticationResult.principal)

                RestAuthenticationToken restAuthenticationToken = new RestAuthenticationToken(authenticationResult.principal, authenticationResult.credentials, authenticationResult.authorities, tokenValue)
                authenticationSuccessHandler.onAuthenticationSuccess(httpServletRequest, httpServletResponse, restAuthenticationToken)
            }else{
                log.debug "Not authenticated. Rest authentication token not generated."
            }

        } else {
            chain.doFilter(request, response)
        }


    }
}

