interface Connection {
    public float getValue();
    public boolean getFired();
    public void setNetwork(Network net);
    public Connection clone(Network net);
}