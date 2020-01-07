package com.comp6776ass2;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Random;
import java.util.*;
import java.lang.Math;

/* This program is coded for COMP6776 Assignment2 (Travelling Salesman Problem) using standard java library
Author: Xiaolong Liu @ Memorial University
Date, February 9, 208
*/

public class TSP {

    public static void main(String[] args) {
        TSP tsp = new TSP();
        tsp.run();
    }

    //main variables
    List<City> cities;
    List<Permutation> population;
    List<Permutation> offSprings;
    List<Permutation> parents;
    List<Double> bestFitList=new ArrayList<>();
    List<Double> worstFitList=new ArrayList<>();
    List<Double> aveFitList=new ArrayList<>();
    double bestFit;
    double worstFit;
    double aveFit;
    Inputs inputs;

    int OFFSPRING_NUM;

    //program run
    public void run() {

        Boolean terminate = false;

        //main program loop
        while (!terminate) {

            //read cities from file
            cities = readFile();

            //read inputs
            inputs = readInput();

            //initialization
            population = initialization(inputs.popSize);

            //Generation loop
            int gen = 1;
            long timeStart=System.nanoTime();

            //evolution starts
            while (gen <= inputs.maxGen) {

                //recombine to get offsprings
                offSprings = recombine(population);

                //mutation
                mutation(offSprings);

                //survivor selection
                survive(population,offSprings);

                //output fitness for each generation
                bestFit = population.get(0).fitness;
                worstFit = population.get(population.size()-1).fitness;
                aveFit = getAveFit(population);
                System.out.println(gen+"th Generations:");
                System.out.println("Best fitness:"+bestFit);
                System.out.println("Worst fitness:"+worstFit);
                System.out.println("Average fitness:"+aveFit);
                System.out.println("-----------------------------------------------");
                System.out.println();

                //collect data
                bestFitList.add(bestFit);
                worstFitList.add(worstFit);
                aveFitList.add(aveFit);

                //go to next generation
                gen++;

            }

            //evolution terminates
            //log runtime
            long timeEnd=System.nanoTime();

            //output the best solution permutation
            System.out.println("Evolution terminated...");
            System.out.println("-----------------------------------------------");
            System.out.println();
            System.out.println("Final best solution permutation is:");
            System.out.println(population.get(0));
            System.out.println();
            System.out.println("Population size: "+ inputs.popSize);
            System.out.println("Mutation rate: "+ inputs.mutationRate);
            System.out.println("Crossover rate: "+ inputs.crossRate);
            System.out.println();
            System.out.println("Final best solution fitness is: "+population.get(0).fitness);
            System.out.println();
            System.out.println("Elapsed time for this run is: "+(timeEnd-timeStart)*Math.pow(10,-9)+" seconds");
            System.out.println("-----------------------------------------------");

            //draw the cities
            CityPlot ctyPlot=new CityPlot(population.get(0));

            //output data
            File f = new File( "bestFit.txt");
            try (PrintWriter pw = new PrintWriter(f)) {
                for (Double d : bestFitList) {
                    pw.println(d);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            f = new File( "worstFit.txt");
            try (PrintWriter pw = new PrintWriter(f)) {
                for (Double d : worstFitList) {
                    pw.println(d);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            f = new File( "aveFit.txt");
            try (PrintWriter pw = new PrintWriter(f)) {
                for (Double d : aveFitList) {
                    pw.println(d);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            //program termination option
            Scanner input = new Scanner(System.in);
            String reset=new String();
            terminate=true;
            while(!("y".equals(reset)|"n".equals(reset))) {
                System.out.println("Do another run? ('y') or 'n' to quit program.");
                reset = input.nextLine();
                if(!("y".equals(reset)|"n".equals(reset))){
                    System.out.println("Incorrect input...");
                } else if ("y".equals(reset)){terminate=false;}
            }
        }
    }


    //read file
    public List<City> readFile() {

        List<City> cities = new ArrayList<>();
        Scanner input = new Scanner(System.in);
        Scanner reader;
        System.out.println("Please type the city source file name:");
        String fileName = input.nextLine();
        File file = new File("./"+fileName);

        if (!file.exists()) {
            System.out.println("File name is incorrect, please input again...");
            readFile();
        }
        try {
            reader = new Scanner(file);
            int count = 0;
            while (reader.hasNextDouble()) {
                reader.nextDouble();
                count++;
                cities.add(new City(count, reader.nextDouble(), reader.nextDouble()));
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return cities;
    }

    //read parameter input
    public Inputs readInput() {

        Inputs inputs = new Inputs();
        Scanner input = new Scanner(System.in);

        //input population sizee
        System.out.println("Population size:");
        inputs.popSize = Integer.parseInt(input.nextLine());
        OFFSPRING_NUM=inputs.popSize;

        //input mutation rate (0~1)
        System.out.println("Mutation rate:");
        inputs.mutationRate = Double.parseDouble(input.nextLine());

        //input crossover rate (0~1)
        System.out.println("Crossover rate:");
        inputs.crossRate = Double.parseDouble(input.nextLine());

        //input max generation count
        System.out.println("Termination condition (max generation):");
        inputs.maxGen = Integer.parseInt(input.nextLine());

        return inputs;
    }

    //population initialization
    public List<Permutation> initialization(int popsize) {

        List<Permutation> pop = new ArrayList<>();
        for (int i = 0; i < popsize; i++) {
            List<City> permutation = new ArrayList<>();
            Random rand = new Random();
            int start = rand.nextInt(cities.size());
            pop.add(new Permutation(copy(cities), start));
        }
        return pop;
    }

    //select parents and do recombination
    public List<Permutation> recombine(List<Permutation> pop) {

        List<Permutation> offsp = new ArrayList<>(OFFSPRING_NUM);
        //generate offsprings
        for (int i=0;i<OFFSPRING_NUM/2;i++){
            Random rand = new Random();
            int index = rand.nextInt(pop.size());
            Permutation p1=pop.get(index);
            index = rand.nextInt(pop.size());
            Permutation p2=pop.get(index);

            //do crossover according to the cross rate
            int num= rand.nextInt(11-1)+1;
            if(num<=inputs.crossRate*10){
                partialMapCrossover(offsp,p1,p2);
            }
            else{
                offsp.add(new Permutation(p1));
                offsp.add(new Permutation(p2));
            }
        }
        return offsp;
    }

    //Partial Mapping Crossover
    public void partialMapCrossover(List<Permutation> offsp, Permutation p1,Permutation p2){

        //random set segment length for crossover
        Permutation osp1=new Permutation(cities.size());
        Permutation osp2=new Permutation(cities.size());
        Permutation segment1=new Permutation();
        Permutation segment2=new Permutation();
        Random rand = new Random();
        int max=cities.size()-2;
        int min=1;
        int segLength=rand.nextInt(max-min) + min;

        //random generate first crossover point
        int crossPoint=rand.nextInt(cities.size()-segLength+1);

        //initialize crossover segments, put segments element into springs first
        for (int i=0;i<segLength;i++) {
            osp1.permutation.set(crossPoint + i, new City(p1.permutation.get(crossPoint + i)));
            osp2.permutation.set(crossPoint + i, new City(p2.permutation.get(crossPoint + i)));
            segment1.permutation.add(p1.permutation.get(crossPoint + i));
            segment2.permutation.add(p2.permutation.get(crossPoint + i));
        }

        //check and get the index in parent2 to put into offspring 1, return -1 if the the element is in p1 segment.
        for(int i=0;i<segLength;i++){
            int currntPoint=crossPoint+i;
            if(!segment1.permutation.contains(p2.permutation.get(currntPoint))) {
                int index = check(currntPoint, segment1, segment2,p1,p2);
                osp1.permutation.set(index,p2.permutation.get(currntPoint));
            }
            if(!segment2.permutation.contains(p1.permutation.get(currntPoint))){
                int index = check(currntPoint, segment2, segment1,p2,p1);
                osp2.permutation.set(index,p1.permutation.get(currntPoint));
            }
        }

        for (int i=0;i<cities.size();i++){
            //for (int i=0;i<9;i++){
            if(osp1.permutation.get(i).id==0){
                osp1.permutation.set(i,new City(p2.permutation.get(i)));
            }
            if(osp2.permutation.get(i).id==0){
                osp2.permutation.set(i,new City(p1.permutation.get(i)));
            }
        }
        osp1.setFitness();
        osp2.setFitness();
        offsp.add(osp1);
        offsp.add(osp2);
    }

    //Check the placement for Partial Mapping Crossover
    public int check(int cp, Permutation sega, Permutation segb, Permutation pa, Permutation pb){

        City pointer=pa.permutation.get(cp);
        if(segb.permutation.contains(pointer)){
            int index=check(pb.permutation.indexOf(pointer),sega,segb,pa,pb);
            return index;
        }
        return pb.permutation.indexOf(pointer);
    }

    //mutation of offsprings
    public void mutation(List<Permutation> offsp) {

        for(Permutation perm:offsp){
            //do mutation according to mutation rate
            Random rand=new Random();
            int num= rand.nextInt(11-1)+1;
            if(num<=inputs.mutationRate*10){
                int i=rand.nextInt(cities.size());
                int j=rand.nextInt(cities.size());
                while (i==j){
                    j=rand.nextInt(cities.size());
                }
                Collections.swap(perm.permutation,i,j);
            }
        }
    }

    //survive selection among population plus new offsprings
    public void survive(List<Permutation> pop, List<Permutation> offsp) {

        pop.addAll(offsp);
        Collections.sort(pop);
        int count=0;
        while(count<inputs.popSize){
            pop.remove(pop.get(pop.size()-1));
            count++;
        }
    }


    //get average fitness
    public double getAveFit(List<Permutation> pop) {

        double avefit=0;
        for(Permutation perm:pop){
            avefit+=perm.fitness;
        }
        return avefit/pop.size();
    }

    //displace all city information
    public void show(List<City> c) {
        for (City cc : c) {
            System.out.println("City ID:" + cc.id + " x:" + cc.x + ", y:" + cc.y);
        }
    }

    //copy permutations
    public List<City> copy(List<City> c) {
        List<City> cities = new ArrayList<>();
        for (City cc : c) {
            cities.add(new City(cc));
        }
        return cities;
    }

    //City class
    class City {

        public int id;
        public double x;
        public double y;

        public City(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }

        public City(City city) {
            this.id = city.id;
            this.x = city.x;
            this.y = city.y;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof City && this.id == (((City) obj).id);
        }
    }

    //input class
    class Inputs {
        public int popSize;
        public int maxGen;
        public double mutationRate;
        public double crossRate;
    }

    //Permutation class
    class Permutation implements Comparable {

        List<City> permutation;
        double fitness;

        public Permutation(){permutation=new ArrayList<>();}

        public Permutation(Permutation perm){
            permutation=new ArrayList<>();
            for(City city:perm.permutation){
                permutation.add(new City(city));
            }
            setFitness();
        }

        //constructor for an empty permutation with specified size
        public Permutation(int size){permutation=new ArrayList<>(size); while(permutation.size() < size) permutation.add(new City(0,0,0)); }

        //constructor using a city array and start id of the city
        public Permutation(List<City> cities, int start) {
            permutation = new ArrayList<>();
            setPermutation(cities, start);
            setFitness();
        }

        //add city to permutation
        public void setPermutation(List<City> cities, int start) {
            Random rand = new Random();
            permutation.add(new City(cities.get(start)));
            cities.remove(start);
            if (cities.isEmpty()) {
                return;
            }
            int nextStart = rand.nextInt(cities.size());
            setPermutation(cities, nextStart);
        }

        //calculation fitness for each permutation
        public void setFitness() {
            for (int i = 0; i < permutation.size(); i++) {
                double dx = 0;
                double dy = 0;
                if (i < permutation.size() - 1) {
                    dx = permutation.get(i).x - permutation.get(i + 1).x;
                    dy = permutation.get(i).y - permutation.get(i + 1).y;
                } else if (i == permutation.size() - 1) {
                    dx = permutation.get(i).x - permutation.get(0).x;
                    dy = permutation.get(i).y - permutation.get(0).y;
                }
                fitness += Math.sqrt(Math.pow((dx), 2) + Math.pow((dy), 2));
            }
        }

        //get max x coordinate among all cities
        public double maxX(){
            double max=permutation.get(0).x;
            for(City cty:permutation){
                if(cty.x>max){
                    max=cty.x;
                }
            }
            return max;
        }

        public double maxY(){
            double max=permutation.get(0).y;
            for(City cty:permutation){
                if(cty.y>max){
                    max=cty.y;
                }
            }
            return max;
        }

        public double minX(){
            double min=permutation.get(0).x;
            for(City cty:permutation){
                if(cty.x<min){
                    min=cty.x;
                }
            }
            return min;
        }
        public double minY(){
            double min=permutation.get(0).y;
            for(City cty:permutation){
                if(cty.y<min){
                    min=cty.y;
                }
            }
            return min;
        }


        @Override
        public int compareTo(Object t) {

            if (this.fitness < ((Permutation) t).fitness) {
                return -1;
            } else if (((Permutation) t).fitness < this.fitness) {
                return 1;
            }
            return 0;
        }

        public String toString(){

            int count=0;
            StringBuilder str=new StringBuilder();
            str.append("[ ");
            for(int i=0;i<this.permutation.size();i++){
                if(i<this.permutation.size()-1){
                    str.append(permutation.get(i).id+", ");
                    count++;
                    if(count==29){
                        str.append("\n");
                        count=0;
                    }
                }
            }
            str.append(permutation.get(permutation.size()-1).id+" ]");
            return str.toString();
        }
    }

    //JFrame class for drawing the cities
    class CityPlot extends JFrame{
        Permutation perm;
        int maxX;
        int maxY;
        int minX;
        int minY;
        int scale=600;

        public CityPlot(Permutation perm){

            this.setTitle("Final Best Permutation");
            this.setCities(perm);
            this.setLayout(null);
            this.pack();
            this.setVisible(true);
            this.setBackground(Color.white);
            this.maxX=(int)perm.maxX();
            this.maxY=(int)perm.maxY();
            this.minX=(int)perm.minX();
            this.minY=(int)perm.minY();
            this.setSize(new Dimension(scale+100, scale+100));
            this.toFront();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            for(int i=0;i<perm.permutation.size();i++){
                g.setColor(Color.black);
                if(i==0 | i==perm.permutation.size()-1){
                    g.setColor(Color.red);
                }
                City cty=perm.permutation.get(i);
                int x=(int) (((cty.x-minX)/(maxX-minX))*(scale)+50);
                int y=(int) (((cty.y-minY)/(maxY-minY))*(scale)+50);
                g.fillOval(x,y,6,6);
                g.drawString(Integer.toString(cty.id),x,y);
                //draw lines
                if(i<perm.permutation.size()-1){
                    City next=perm.permutation.get(i+1);
                    int nextx=(int) (((next.x-minX)/(maxX-minX))*(scale)+50);
                    int nexty=(int) (((next.y-minY)/(maxY-minY))*(scale)+50);
                    g.setColor(Color.blue);
                    g.drawLine(x+3,y+3,nextx+3,nexty+3);
                }
            }
        }

        public void setCities(Permutation perm) {
            this.perm = perm;
        }
    }
}
