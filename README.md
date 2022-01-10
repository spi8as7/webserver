# webserver

Developed using Netbeans IDE

## Build

- Build Project
- File->Project Properties(Webserver)->Run


```
<?xml version="1.0" encoding="UTF-8" ?>
<ce325server>
  <listen port="8000" />
  <statistics port="8001" />
  <log>
    <access filepath="C:\\Users\\spi8a_000\\Desktop\\logs.txt" />
    <error filepath="C:\\Users\\spi8a_000\\Desktop\\errors.txt" />
  </log>
  <documentroot filepath="C:\\Users\\spi8a_000\\Desktop\\server" />
  <runphp>no</runphp>
  <denyaccess>
    <ip>52.8.64.0/24</ip>
    <ip>128.44.0.0/16</ip>
  </denyaccess>
</ce325server>
```
