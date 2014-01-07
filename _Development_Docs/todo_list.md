- IMPORTANT comment code
- fix problem with synchronisation, one mistake is if calculation on client is started and project is uploaded after then,
  command for starting calculation has to be typed again
- set priority ranges which will be available
- problem with putting tasks to task pool on server after failure, for example:
  change timeout of client active ping to higher number, maybe 3 minuted, because as a assumption, tasks should take about
  15 minutes, so it the client will disconnect during that 3 minutes but connected immediately ( only network error), then there
  is no need to cancel tasks association for tasks calculated by this client
- implement server state saving - pause all calculation, wait for rest of tasks and synchronise all information to disk
