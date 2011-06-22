#!/usr/bin/python2
from lxml import etree
import os

def run():
    results = [f for f in os.listdir(".") if os.path.isdir(f)]
    count = {}
    statuses = {}
    for result in results:
        print result
        tree = etree.parse(result + "/report.html")
        for row in tree.getroot().findall("table")[0].findall("tr"):
            cells = row.findall("td")
            if len(cells) == 3:
                if cells[0].text not in statuses:
                    statuses[cells[0].text] = []
                statuses[cells[0].text].append(cells[1].text)
        dat = [f for f in os.listdir(result) if os.path.isfile(result + "/" + f) and f[-3:] == 'dat']
        print dat
        for d in dat:
            k = d.split('.')[0]
            if k not in count:
                count[k]=0;
            count[k] = count[k] + 1
    print count
    for status in statuses.iteritems():
        print status
if __name__ == '__main__':
    run()