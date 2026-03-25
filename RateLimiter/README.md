Rate Limiter 

Question should come to mind before starting the problem 

1. How configuration will come to system . Will there be any API or shall we assume loading at startup from DB or hardcoded ?
2. What would the inpur for API ? client id , end point name , enviorment.
3. How many types of RateLimiter Algorith is supported ?
4. Is there any default Rate LimiterAlgorithm ?
5. How the result would display ?


Entities Identification 

1. RateLimiterService -> It works as orchestrator.
2. Rate Limiter Key -> it could have client id , end point and enviorment
3. Rate Limiter configuration -> Different configuration for different algorithm.
4. Rate Limiter algorithm -> token , fixed and window.
5. Any enum needed ? -> AlgorithmType
6. Result in human understable manner.


