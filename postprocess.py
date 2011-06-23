#!/usr/bin/python2
from lxml import etree
import os

def run(directory):
    results = [f for f in os.listdir(directory) if os.path.isdir(directory + '/' + f)]
    count = {}
    statuses = {}
    outliers_degree = {}
    for result in results:
        print result
        # Load triples
        sameas = {}
        for line in open('data-latc/' + result).readlines():
            elmts = line.split(' ')
            if len(elmts) > 0:
                if elmts[0][1:-1] not in sameas:
                    sameas[elmts[0][1:-1]] = 0
                sameas[elmts[0][1:-1]] = sameas[elmts[0][1:-1]] + 1
        tree = etree.parse(directory + '/' + result + "/report.html")
        # Scores
        for row in tree.getroot().findall("table")[0].findall("tr"):
            cells = row.findall("td")
            if len(cells) == 3:
                if cells[0].text not in statuses:
                    statuses[cells[0].text] = []
                statuses[cells[0].text].append(cells[1].text)
        # Outliers
        for row in tree.getroot().findall("table")[1].findall("tr"):
            cells = row.findall("td")
            if len(cells) > 0:
                if cells[0].text == 'Degree':
                    for cell in cells[1:]:
                        ratio = cell.text.split(' ')[1].split(' ')[0][1:-1]
                        print ratio + ' -> ' + str(sameas[cell.text.split(' ')[0]])
                        if ratio not in outliers_degree:
                            outliers_degree[ratio] = []
                        outliers_degree[ratio].append(sameas[cell.text.split(' ')[0]])
        dat = [f for f in os.listdir(directory + '/' + result) if os.path.isfile(directory + '/' + result + "/" + f) and f[-3:] == 'dat']
        for d in dat:
            k = d.split('.')[0]
            if k not in count:
                count[k] = 0;
            count[k] = count[k] + 1
    print count
    for status in statuses.iteritems():
        print status
    for (d, o) in outliers_degree.iteritems():
        print '%s %d' % (d, len(o))
        
if __name__ == '__main__':
    run('geonames-reports-onlyout')
    
