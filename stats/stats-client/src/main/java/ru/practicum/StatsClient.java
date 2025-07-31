package ru.practicum;





@FeignClient(name = "stats-server")
public interface StatsClient {
    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    void hit(@Valid @RequestBody StatsDtoRequest hitRequest) throws FeignException;

    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    List<StatsDtoResponse> getStatistics(@RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                    @RequestParam(required = false) List<String> uris,
                                    @RequestParam(defaultValue = "false") boolean unique) throws FeignException;
}